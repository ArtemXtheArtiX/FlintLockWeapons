package com.siliwood;

import com.siliwood.handler.KeyHandler;
import com.siliwood.render.RenderWeaponFP;

/**
 * Client-side setup: key bindings + first-person 3D weapon renderer.
 */
public class ClientProxy extends Proxy
{
    @Override
    public void preInit()
    {
        KeyHandler.register();
    }

    @Override
    public void init()
    {
        RenderWeaponFP.register();
    }

    @Override
    public void postInit()
    {
        SiliwoodMod.registerItemModels();
    }
}
