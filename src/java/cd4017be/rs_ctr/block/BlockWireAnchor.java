package cd4017be.rs_ctr.block;

import cd4017be.lib.block.AdvancedBlock;
import cd4017be.rs_ctr.api.wire.IHookAttachable;
import cd4017be.rs_ctr.api.wire.RelayPort;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;


/**
 * @author CD4017BE
 *
 */
public class BlockWireAnchor extends AdvancedBlock {

	/**
	 * @param id
	 * @param m
	 * @param sound
	 * @param flags
	 * @param tile
	 */
	public BlockWireAnchor(String id, Material m, SoundType sound, int flags, Class<? extends TileEntity> tile) {
		super(id, m, sound, flags, tile);
		setBlockBounds(NULL_AABB);
	}

	@Override
	public RayTraceResult collisionRayTrace(IBlockState state, World world, BlockPos pos, Vec3d start, Vec3d end) {
		double x0 = 1, x1 = 0, y0 = 1, y1 = 0, z0 = 1, z1 = 0;
		TileEntity te = world.getTileEntity(pos);
		if (te instanceof IHookAttachable)
			for (RelayPort port : ((IHookAttachable)te).getHookPins().values()) {
				if (!port.isSource) continue;
				double d = port.pos.x;
				if (d < x0) x0 = d;
				if (d > x1) x1 = d;
				d = port.pos.y;
				if (d < y0) y0 = d;
				if (d > y1) y1 = d;
				d = port.pos.z;
				if (d < z0) z0 = d;
				if (d > z1) z1 = d;
			}
		return rayTrace(pos, start, end, new AxisAlignedBB(x0 - 0.0625, x1 + 0.0625, y0 - 0.0625, y1 + 0.0625, z0 - 0.0625, z1 + 0.0625));
	}

}