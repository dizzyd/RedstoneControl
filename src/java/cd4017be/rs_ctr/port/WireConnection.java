package cd4017be.rs_ctr.port;

import java.util.List;

import cd4017be.api.rs_ctr.interact.IInteractiveComponent.IBlockRenderComp;
import cd4017be.api.rs_ctr.interact.IInteractiveComponent.ITESRenderComp;
import cd4017be.api.rs_ctr.port.IPortProvider;
import cd4017be.api.rs_ctr.port.ITagableConnector;
import cd4017be.api.rs_ctr.port.MountedPort;
import cd4017be.api.rs_ctr.port.Port;
import cd4017be.api.rs_ctr.wire.IWiredConnector;
import cd4017be.api.rs_ctr.wire.RelayPort;
import cd4017be.lib.util.Orientation;
import cd4017be.rs_ctr.render.PortRenderer;
import cd4017be.rs_ctr.render.WireRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 
 * @author CD4017BE
 */
public class WireConnection extends Plug implements ITagableConnector, IWiredConnector, IBlockRenderComp, ITESRenderComp {

	public static final String ID = "wire";

	private final WireType type;
	BlockPos linkPos;
	int linkPin;
	Vec3d line;
	private int count;
	private String tag;

	public WireConnection(WireType type) {
		this.type = type;
	}

	public WireConnection(BlockPos linkPos, int linkPin, Vec3d line, int count, WireType type) {
		this(type);
		this.linkPos = linkPos;
		this.linkPin = linkPin;
		this.line = line;
		this.count = count;
	}

	@Override
	protected String id() {
		return type.wiredId;
	}

	@Override
	public NBTTagCompound serializeNBT() {
		NBTTagCompound nbt = super.serializeNBT();
		nbt.setLong("pos", linkPos.toLong());
		nbt.setInteger("pin", linkPin);
		nbt.setFloat("dx", (float)line.x);
		nbt.setFloat("dy", (float)line.y);
		nbt.setFloat("dz", (float)line.z);
		nbt.setByte("count", (byte)count);
		if (tag != null) nbt.setString("tag", tag);
		return nbt;
	}

	@Override
	public void deserializeNBT(NBTTagCompound nbt) {
		count = nbt.getByte("count") & 0xff;
		line = new Vec3d(nbt.getFloat("dx"), nbt.getFloat("dy"), nbt.getFloat("dz"));
		linkPos = BlockPos.fromLong(nbt.getLong("pos"));
		linkPin = nbt.getInteger("pin");
		tag = nbt.hasKey("tag", NBT.TAG_STRING) ? nbt.getString("tag") : null;
	}

	@Override
	public void onRemoved(MountedPort port, EntityPlayer player) {
		super.onRemoved(port, player);
		IWiredConnector.super.onRemoved(port, player);
		port.disconnect();
	}

	@Override
	protected ItemStack drop() {
		return new ItemStack(type.wireItem, count);
	}

	@Override
	public void onLinkMove(MountedPort host, MountedPort link) {
		line = IWiredConnector.getPath(host, link).scale(.5);
		host.owner.onPortModified(host, IPortProvider.E_CON_UPDATE);
	}

	private float[] vertices; //render cache
	private int light1 = -1;

	@Override
	@SideOnly(Side.CLIENT)
	public void render(World world, BlockPos pos, double x, double y, double z, int light, BufferBuilder buffer) {
		if (vertices == null) vertices = WireRenderer.createLine(port, line);
		if (light1 < 0) light1 = port.getWorld().getCombinedLight(port.getPos().add(line.x + port.pos.x, line.y + port.pos.y, line.z + port.pos.z), 0);
		WireRenderer.drawLine(buffer, vertices, (float)x, (float)y, (float)z, light, light1, type.color);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void render(List<BakedQuad> quads) {
		this.light1 = -1;
		if (port instanceof RelayPort) return;
		PortRenderer.PORT_RENDER.drawModel(quads, (float)port.pos.x, (float)port.pos.y, (float)port.pos.z, Orientation.fromFacing(port.face), type.model);
	}

	@Override
	public AxisAlignedBB getRenderBB(World world, BlockPos pos) {
		Vec3d p = new Vec3d(pos).add(port.pos);
		return new AxisAlignedBB(p, p.add(line));
	}

	@Override
	public void setTag(MountedPort port, String tag) {
		this.tag = tag;
		port.owner.onPortModified(port, IPortProvider.E_CON_UPDATE);
	}

	@Override
	public String getTag() {
		return tag;
	}

	@Override
	public boolean isCompatible(Class<?> type) {
		return type == this.type.clazz;
	}

	@Override
	public Port getLinkPort(MountedPort from) {
		return IPortProvider.getPort(from.getWorld(), linkPos, linkPin);
	}

	@Override
	public boolean isLinked(MountedPort to) {
		return to.pin == linkPin && to.getPos().equals(linkPos);
	}

}