/**
 * This file is part of FoamFixAPI.
 *
 * FoamFixAPI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FoamFixAPI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with FoamFixAPI.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Additional permission under GNU GPL version 3 section 7
 *
 * If you modify this Program, or any covered work, by linking or
 * combining it with the Minecraft game engine, the Mojang Launchwrapper,
 * the Mojang AuthLib and the Minecraft Realms library (and/or modified
 * versions of said software), containing parts covered by the terms of
 * their respective licenses, the licensors of this Program grant you
 * additional permission to convey the resulting work.
 */
package pl.asie.environmentchecker;

import java.io.File;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.asm.ASMTransformerWrapper;
import net.minecraftforge.fml.relauncher.IFMLCallHook;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import pl.asie.environmentchecker.tracker.TrackingClassTransformerWrapper;
import pl.asie.environmentchecker.tracker.TransformerRewrapper;

@IFMLLoadingPlugin.Name("I don't touch any classes please leave me alone ;_;")
@IFMLLoadingPlugin.SortingIndex(Integer.MAX_VALUE)
@IFMLLoadingPlugin.TransformerExclusions("pl.asie.environmentchecker")
public class EnvCheckCore implements IFMLLoadingPlugin, IFMLCallHook {
    private static boolean showForgeTransformers = false;

    public String[] getASMTransformerClass() {
        return new String[] {
                "pl.asie.environmentchecker.tracker.TransformerRewrapper"
        };
    }
    
    public String getModContainerClass() {
        return null;
    }
    
    public String getSetupClass() {
        return "pl.asie.environmentchecker.EnvCheckCore";
    }
    
    public void injectData(final Map<String, Object> data) {

    }
    
    public String getAccessTransformerClass() {
        return null;
    }

    public static void wrapTransformers() throws IllegalAccessException {
        LaunchClassLoader classLoader = (LaunchClassLoader) EnvCheckCore.class.getClassLoader();

        // Not so simple!
        Field transformersField = ReflectionHelper.findField(LaunchClassLoader.class, "transformers");
        List<IClassTransformer> transformerList = (List<IClassTransformer>) transformersField.get(classLoader);

        for (int i = 0; i < transformerList.size(); i++) {
            IClassTransformer transformer = transformerList.get(i);
            IClassTransformer parentTransformer = transformer;
            if (transformer instanceof ASMTransformerWrapper.TransformerWrapper) {
                Field parentTransformerField = ReflectionHelper.findField(ASMTransformerWrapper.TransformerWrapper.class, "parent");
                parentTransformer = (IClassTransformer) parentTransformerField.get(transformer);
            }

            if (parentTransformer == null || parentTransformer instanceof TrackingClassTransformerWrapper
                    || parentTransformer instanceof TransformerRewrapper)
                continue;

            if (!showForgeTransformers) {
                if (parentTransformer.getClass().getName().startsWith("net.minecraftforge")
                        || parentTransformer.getClass().getName().startsWith("cpw.mods.fml") /* 1.7 compat */) {
                    continue;
                }
            }
            transformerList.set(i, new TrackingClassTransformerWrapper(transformer));
        }
    }

    @Override
    public Void call() throws Exception {
        File file = new File("envcheck");
        if (!file.exists()) {
            file.mkdir();
        }

        Configuration config = new Configuration(new File(new File("config"), "envcheck.cfg"));
        showForgeTransformers = config.getBoolean("showForgeTransformers", "asmTransformerChanges", false, "Whether or not to show the changes done by Forge's own transformers.");;
        config.save();

        wrapTransformers();

        return null;
    }
}
