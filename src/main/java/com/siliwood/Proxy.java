package com.siliwood;

/**
 * Common proxy. All registration happens in common code; the client proxy
 * only layers client-only setup (key bindings, renderers, HUD) on top.
 */
public class Proxy
{
    public void preInit() {}

    public void init() {}

    public void postInit() {}
}
