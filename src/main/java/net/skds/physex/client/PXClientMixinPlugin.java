package net.skds.physex.client;

import net.skds.physex.PXMixinPlugin;
import net.skds.physex.PhysExBootConfig;

public class PXClientMixinPlugin extends PXMixinPlugin {

	@Override
	public boolean shouldApplyMixin(String target, String mixin) {
		if (PhysExBootConfig.INSTANCE.getServerOnly().isEnabled()) return false;
		return super.shouldApplyMixin(target, mixin);
	}
}
