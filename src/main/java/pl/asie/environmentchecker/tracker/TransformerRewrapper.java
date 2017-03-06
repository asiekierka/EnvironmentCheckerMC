package pl.asie.environmentchecker.tracker;

import net.minecraft.launchwrapper.IClassTransformer;
import pl.asie.environmentchecker.EnvCheck;
import pl.asie.environmentchecker.EnvCheckCore;

/**
 * Created by asie on 3/6/17.
 */
public class TransformerRewrapper implements IClassTransformer {
	private boolean wrapped = false;

	@Override
	public byte[] transform(String name, String transformedName, byte[] basicClass) {
		if (!wrapped) {
			try {
				EnvCheckCore.wrapTransformers();
			} catch (IllegalAccessException e) {

			}
			wrapped = true;
		}
		return basicClass;
	}
}
