package com.siliwood.weapon;

/**
 * Sequential reload stages. A weapon remembers the stage it was left at,
 * so if the player swaps slots mid-reload, progress is kept in NBT and
 * reloading can be resumed from the same stage later.
 */
public enum ReloadStage
{
    NONE(0),
    POUR_POWDER(1),      // насыпание пороха / заряжание патрона (дульнозарядные)
    RAM_PROJECTILE(2),    // закидывание снаряда + проталживание шомполом (дульнозарядные)
    CYCLE_ACTION(3);      // передёргивание затвора / взведение курка (у всех)

    public final int id;

    ReloadStage(int id)
    {
        this.id = id;
    }

    public static ReloadStage fromId(int id)
    {
        for (ReloadStage s : values())
            if (s.id == id) return s;
        return NONE;
    }
}
