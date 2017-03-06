package pl.asie.environmentchecker.tracker;

import net.minecraft.launchwrapper.IClassTransformer;

import java.util.Arrays;

public class TrackingClassTransformerWrapper implements IClassTransformer {
	private final IClassTransformer parent;
	private final String id;

	public TrackingClassTransformerWrapper(IClassTransformer transformer) {
		this.parent = transformer;
		String className = transformer.getClass().getName();
		if (className.startsWith("$wrapper.")) {
			className = className.substring("$wrapper.".length());
		}
		this.id = className;
		TransformerTracker.INSTANCE.transformers.add(id);
	}

	@Override
	public byte[] transform(String name, String transformedName, byte[] data) {
		byte[] dataOld = data.clone();
		long timeOld = System.nanoTime();
		byte[] dataNew = parent.transform(name, transformedName, data);
		long time = System.nanoTime() - timeOld;
		TransformerTracker.INSTANCE.timeUsedByMod.adjustOrPutValue(id, time, time);
		if (!Arrays.equals(dataOld, dataNew)) {
			try {
				TransformerTracker.INSTANCE.add(id, name, transformedName, dataOld, dataNew, time);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return dataNew;
	}
}
