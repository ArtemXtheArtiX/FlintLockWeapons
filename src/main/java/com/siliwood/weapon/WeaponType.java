package com.siliwood.weapon;

/**
 * Mechanically distinct weapon families, modelled after real-world analogues.
 */
public enum WeaponType
{
    /** Мушкет, фузея, ружьё — дульнозарядные: порох -> снаряд+шомпол -> курок */
    MUZZLELOADER("muzzleloader", ReloadStage.NONE, ReloadStage.POUR_POWDER, ReloadStage.RAM_PROJECTILE, ReloadStage.CYCLE_ACTION),
    /** Кремнёвый пистолет — короткий дульнозарядный, те же этапы */
    FLINTLOCK_PISTOL("flintlock_pistol", ReloadStage.NONE, ReloadStage.POUR_POWDER, ReloadStage.RAM_PROJECTILE, ReloadStage.CYCLE_ACTION),
    /** Капсюльное ружьё (перкуссия) — капсюль+порох одним этапом, затем снаряд, курок */
    PERCUSSION("percussion", ReloadStage.NONE, ReloadStage.POUR_POWDER, ReloadStage.RAM_PROJECTILE, ReloadStage.CYCLE_ACTION),
    /** Иглольчатая винтовка Дрейзе — затвор: заряд патрона и передёргивание слиты в один этап */
    NEEDLE_GUN("needle_gun", ReloadStage.NONE, ReloadStage.CYCLE_ACTION, null, null),
    /** Затворная винтовка (Маузер-типа) — магазин + затвор: дослать патрон = передёрнуть */
    BOLT_ACTION("bolt_action", ReloadStage.NONE, ReloadStage.CYCLE_ACTION, null, null),
    /** Двуствольный обрез/дробовик — переломный затвор: зарядить оба ствола, щёлкнуть затвором */
    BREAK_ACTION("break_action", ReloadStage.NONE, ReloadStage.POUR_POWDER, ReloadStage.CYCLE_ACTION, null);

    public final String name;
    /** Этапы, через которые проходит перезарядка данного типа (по порядку). */
    public final ReloadStage[] stages;

    WeaponType(String name, ReloadStage start, ReloadStage... rest)
    {
        this.name = name;
        java.util.List<ReloadStage> list = new java.util.ArrayList<>();
        if (start != ReloadStage.NONE) list.add(start);
        for (ReloadStage s : rest)
            if (s != null) list.add(s);
        // первый "этап" — это переход из NONE; храним полный маршрут
        this.stages = list.toArray(new ReloadStage[0]);
    }

    /** Следующий этап после given, либо null если цикл завершён. */
    public ReloadStage next(ReloadStage current)
    {
        int idx = -1;
        for (int i = 0; i < stages.length; i++)
            if (stages[i] == current) { idx = i; break; }
        if (idx < 0) return stages.length > 0 ? stages[0] : null;
        return idx + 1 < stages.length ? stages[idx + 1] : null;
    }

    /** Первый этап перезарядки. */
    public ReloadStage first()
    {
        return stages.length > 0 ? stages[0] : null;
    }

    public static WeaponType fromName(String n)
    {
        for (WeaponType t : values())
            if (t.name.equals(n)) return t;
        return MUZZLELOADER;
    }
}
