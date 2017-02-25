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
import net.minecraftforge.fml.common.asm.ASMTransformerWrapper;
import net.minecraftforge.fml.relauncher.IFMLCallHook;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import pl.asie.environmentchecker.tracker.TrackingClassTransformerWrapper;

@IFMLLoadingPlugin.Name("I don't touch any classes please leave me alone ;_;")
@IFMLLoadingPlugin.SortingIndex(Integer.MAX_VALUE)
public class EnvCheckCore implements IFMLLoadingPlugin, IFMLCallHook {
    public String[] getASMTransformerClass() {
        return new String[] { };
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

    @Override
    public Void call() throws Exception {
        File file = new File("envcheck");
        if (!file.exists()) {
            file.mkdir();
        }

        LaunchClassLoader classLoader = (LaunchClassLoader) getClass().getClassLoader();

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
            if (parentTransformer == null
                    || parentTransformer.getClass().getName().startsWith("net.minecraftforge")) {
                continue;
            }
            transformerList.set(i, new TrackingClassTransformerWrapper(transformer));
        }

        return null;
    }
}
