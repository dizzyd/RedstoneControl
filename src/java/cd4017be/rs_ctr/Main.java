package cd4017be.rs_ctr;

import java.io.File;
import org.apache.logging.log4j.Logger;

import cd4017be.api.recipes.RecipeScriptContext;
import cd4017be.api.recipes.RecipeScriptContext.ConfigConstants;
import cd4017be.api.rs_ctr.port.Link;
import cd4017be.lib.script.ScriptFiles.Version;
import cd4017be.rs_ctr.tileentity.OC_Adapter;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppedEvent;

/**
 * @author CD4017BE
 *
 */
@Mod(modid = Main.ID, useMetadata = true)
public class Main {

	public static final String ID = "rs_ctr";

	@Instance(ID)
	public static Main instance;

	public static Logger LOG;

	@SidedProxy(clientSide="cd4017be." + ID + ".ClientProxy", serverSide="cd4017be." + ID + ".CommonProxy")
	public static CommonProxy proxy;

	public Main() {
		RecipeScriptContext.scriptRegistry.add(new Version("redstoneControl", "/assets/" + ID + "/config/recipes.rcp"));
	}

	@Mod.EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		LOG = event.getModLog();
		proxy.preInit();
		RecipeScriptContext.instance.run("redstoneControl.PRE_INIT");
	}

	@Mod.EventHandler
	public void load(FMLInitializationEvent event) {
		Objects.init();
		proxy.init(new ConfigConstants(RecipeScriptContext.instance.modules.get("redstoneControl")));
		if (Loader.isModLoaded("opencomputers"))
			OC_Adapter.registerAPI();
	}

	@Mod.EventHandler
	public void postInit(FMLPostInitializationEvent event) {
	}

	@Mod.EventHandler
	public void serverStart(FMLServerAboutToStartEvent event) {
		Link.loadData(new File(FMLCommonHandler.instance().getSavesDirectory(), event.getServer().getFolderName()));
	}

	@Mod.EventHandler
	public void serverStop(FMLServerStoppedEvent event) {
		Link.saveData();
	}

}
