package cd4017be.rs_ctr.item;

import cd4017be.api.rs_ctr.port.IConnector.IConnectorItem;
import cd4017be.api.rs_ctr.port.IPortProvider;
import java.util.function.Supplier;
import cd4017be.api.rs_ctr.port.IConnector;
import cd4017be.api.rs_ctr.port.MountedPort;
import cd4017be.lib.item.BaseItem;
import cd4017be.rs_ctr.port.SplitPlug;
import cd4017be.rs_ctr.port.WireType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentTranslation;

/** @author CD4017BE */
public class ItemSplitCon extends BaseItem implements IConnectorItem {

	public final WireType type;
	public final Supplier<SplitPlug> constructor;

	public ItemSplitCon(String id, WireType type, Supplier<SplitPlug> constructor) {
		super(id);
		this.type = type;
		this.constructor = constructor;
		IConnector.REGISTRY.put(type.splitId, constructor);
	}

	@Override
	public void doAttach(ItemStack stack, MountedPort port, EntityPlayer player) {
		if(port.type != type.clazz) {
			player.sendMessage(new TextComponentTranslation("msg.rs_ctr.type"));
			return;
		} else if(!port.isMaster) {
			player.sendMessage(new TextComponentTranslation("msg.rs_ctr.split"));
			return;
		}
		int n;
		IConnector con = port.getConnector();
		if(con instanceof SplitPlug) {
			SplitPlug sp = (SplitPlug)con;
			n = sp.addLinks(1);
			if(n > 0) port.owner.onPortModified(port, IPortProvider.E_CON_UPDATE);
		} else {
			SplitPlug sp = constructor.get();
			if (player.isCreative() || (n = stack.getCount()) > 2) n = 2;
			n = sp.addLinks(n);
			port.setConnector(sp, player);
		}
		if(!player.isCreative()) stack.shrink(n);
	}

}
