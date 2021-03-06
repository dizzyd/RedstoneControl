package cd4017be.rs_ctr.port;

import java.util.Arrays;
import cd4017be.api.rs_ctr.com.SignalHandler;
import cd4017be.rs_ctr.Objects;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/** @author CD4017BE */
public class SignalSplitPlug extends SplitPlug implements SignalHandler {

	SignalHandler[] callbacks;
	int lastValue;

	@Override
	public int addLinks(int n) {
		n = super.addLinks(n);
		callbacks = callbacks == null ? new SignalHandler[n] : Arrays.copyOf(callbacks, links.length);
		Arrays.fill(callbacks, callbacks.length - n, callbacks.length, SignalHandler.NOP);
		return n;
	}

	@Override
	public void deserializeNBT(NBTTagCompound nbt) {
		super.deserializeNBT(nbt);
		callbacks = new SignalHandler[links.length];
		Arrays.fill(callbacks, SignalHandler.NOP);
	}

	@Override
	public void setPortCallback(int pin, Object callback) {
		if(callback instanceof SignalHandler)
			(callbacks[pin] = (SignalHandler)callback).updateSignal(lastValue);
		else callbacks[pin] = SignalHandler.NOP;
	}

	@Override
	protected WireType type() {
		return WireType.SIGNAL;
	}

	@Override
	protected ItemStack drop() {
		return new ItemStack(Objects.split_s, links.length);
	}

	@Override
	public void updateSignal(int value) {
		if(value == lastValue) return;
		lastValue = value;
		for(SignalHandler h : callbacks)
			h.updateSignal(value);
	}

}
