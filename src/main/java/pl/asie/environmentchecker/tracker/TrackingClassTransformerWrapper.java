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
	}

	@Override
	public byte[] transform(String name, String transformedName, byte[] data) {
		byte[] dataOld = data.clone();
		byte[] dataNew = parent.transform(name, transformedName, data);
		if (!Arrays.equals(dataOld, dataNew)) {
			try {
				TransformerTracker.INSTANCE.add(id, name, transformedName, dataOld, dataNew);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return dataNew;
	}
}
