package pl.asie.environmentchecker.tracker;

import com.google.common.base.Joiner;
import com.google.common.collect.*;
import gnu.trove.map.TObjectLongMap;
import gnu.trove.map.hash.TObjectLongHashMap;
import net.minecraftforge.fml.common.asm.transformers.deobf.FMLRemappingAdapter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.commons.RemappingClassAdapter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import pl.asie.environmentchecker.ASMUtil;

import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;

public final class TransformerTracker {
	public static class ClassChangeInfo {
		public final String modName, className;
		public final List<String> subData;
		public final long timeUsed;

		public ClassChangeInfo(String modName, String className, long timeUsed) {
			this.modName = modName;
			this.className = className;
			this.subData = new ArrayList<>();
			this.timeUsed = timeUsed;
		}
	}

	public static final TransformerTracker INSTANCE = new TransformerTracker();
	private static final Joiner commaJoiner = Joiner.on(", ");
	private static final NumberFormat timeFormat = new DecimalFormat("#.###");

	public Set<String> transformers = new HashSet<>();
	public Multimap<String, ClassChangeInfo> changedClassesByMod = TreeMultimap.create(
			Comparator.naturalOrder(),
			Comparator.comparing(classChangeInfo -> classChangeInfo.className)
	);
	public TObjectLongMap<String> timeUsedByMod = new TObjectLongHashMap<>();
	public Multimap<String, ClassChangeInfo> changedClassesByClass = TreeMultimap.create(
			Comparator.naturalOrder(),
			Comparator.comparing(classChangeInfo -> classChangeInfo.modName)
	);

	public boolean delaySaves = true;
	private boolean dirty = false;

	private TransformerTracker() {

	}

	private List<String> twoToThree(List<String> old, List<String> neu) {
		Set<String> all = Sets.newHashSet(old);
		all.addAll(neu);
		List<String> old2 = Lists.newArrayList(old);
		old.removeAll(neu);
		neu.removeAll(old2);
		all.removeAll(old);
		all.removeAll(neu);

		List<String> allList = Lists.newArrayList(all);

		Collections.sort(allList);
		Collections.sort(neu);
		Collections.sort(old);

		return allList;
	}

	public void add(String transformerId, String oldClassName, String className, byte[] dataOld, byte[] dataNew, long timeUsed) {
		if (dataOld == null || dataNew == null) // TODO: handle me?
			return;

		ClassReader readerOld = new ClassReader(dataOld);
		ClassReader readerNew = new ClassReader(dataNew);

		ClassNode nodeOld = new ClassNode();
		ClassNode nodeNew = new ClassNode();

		readerOld.accept(new FMLRemappingAdapter(nodeOld), ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
		readerNew.accept(new FMLRemappingAdapter(nodeNew), ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
		List<String> fieldNamesOld = new ArrayList<>();
		List<String> fieldNamesNew = new ArrayList<>();
		List<String> methodNamesOld = new ArrayList<>();
		List<String> methodNamesNew = new ArrayList<>();
		Map<String, FieldNode> fnmOld = new HashMap<>();
		Map<String, FieldNode> fnmNew = new HashMap<>();
		Map<String, MethodNode> mnmOld = new HashMap<>();
		Map<String, MethodNode> mnmNew = new HashMap<>();
		for (FieldNode fn : nodeOld.fields) {
			fieldNamesOld.add(fn.name + "\n" + fn.desc);
			fnmOld.put(fn.name + "\n" + fn.desc, fn);
		}
		for (FieldNode fn : nodeNew.fields) {
			fieldNamesNew.add(fn.name + "\n" + fn.desc);
			fnmNew.put(fn.name + "\n" + fn.desc, fn);
		}
		for (MethodNode mn : nodeOld.methods) {
			methodNamesOld.add(mn.name + "\n" + mn.desc);
			mnmOld.put(mn.name + "\n" + mn.desc, mn);
		}
		for (MethodNode mn : nodeNew.methods) {
			methodNamesNew.add(mn.name + "\n" + mn.desc);
			mnmNew.put(mn.name + "\n" + mn.desc, mn);
		}

		List<String> fieldNames = twoToThree(fieldNamesOld, fieldNamesNew);
		List<String> methodNames = twoToThree(methodNamesOld, methodNamesNew);

		ClassChangeInfo info = new ClassChangeInfo(transformerId, className, timeUsed);

		for (String s : fieldNamesOld) {
			info.subData.add("Field DEL: " + s.replace('\n', ' '));
		}
		for (String s : fieldNamesNew) {
			info.subData.add("Field ADD: " + s.replace('\n', ' '));
		}
		for (String s : methodNamesOld) {
			info.subData.add("Method DEL: " + s.replace('\n', ' '));
		}
		for (String s : methodNamesNew) {
			info.subData.add("Method ADD: " + s.replace('\n', ' '));
		}
		for (String s : methodNames) {
			MethodNode mnOld = mnmOld.get(s);
			MethodNode mnNew = mnmNew.get(s);
			if (!ASMUtil.equalsOpcodeWise(mnOld.instructions, mnNew.instructions)) {
				info.subData.add("Method CHG: " + s.replace('\n', ' '));
			}
		}

		changedClassesByMod.put(transformerId, info);
		changedClassesByClass.put(className, info);

		if (delaySaves) {
			dirty = true;
		} else {
			saveForced();
		}
	}

	public void save(boolean force) {
		if (dirty || force) {
			saveForced();
			dirty = false;
		}
	}

	private String timeToString(long time) {
		double timeMs = time / 1000000.0;
		return timeFormat.format(timeMs) + " ms";
	}

	private void saveForced() {
		try {
			PrintWriter out = new PrintWriter("./envcheck/asmTransformerChanges.txt");
			out.println("===\nClasses changed (by changing transformer):\n===\n");
			for (String key : transformers) {
				out.println("- " + key + " (" + timeToString(timeUsedByMod.get(key)) + ")");
				if (changedClassesByMod.containsKey(key)) {
					for (ClassChangeInfo value : changedClassesByMod.get(key)) {
						out.println("\t- " + value.className + " (" + timeToString(value.timeUsed) + ")");
						for (String value2 : value.subData) {
							out.println("\t\t- " + value2);
						}
					}
				}
			}
			out.println("\n\n===\nClasses changed (by changed class):\n===\n");
			for (String key : changedClassesByClass.keySet()) {
				out.println("- " + key);
				for (ClassChangeInfo value : changedClassesByClass.get(key)) {
					out.println("\t- " + value.modName);
					for (String value2 : value.subData) {
						out.println("\t\t- " + value2);
					}
				}
			}

			out.flush();
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
