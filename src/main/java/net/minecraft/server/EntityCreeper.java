package net.minecraft.server;

// CraftBukkit start
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.entity.CreeperPowerEvent;
// CraftBukkit end

public class EntityCreeper extends EntityMonster {

    int fuseTicks;
    int b;

    public EntityCreeper(World world) {
        super(world);
        this.texture = "/mob/creeper.png";
    }

    public int getMaxHealth() {
        return 20;
    }

    protected void b() {
        super.b();
        this.datawatcher.a(16, Byte.valueOf((byte) -1));
        this.datawatcher.a(17, Byte.valueOf((byte) 0));
    }

    public void b(NBTTagCompound nbttagcompound) {
        super.b(nbttagcompound);
        if (this.datawatcher.getByte(17) == 1) {
            nbttagcompound.setBoolean("powered", true);
        }
    }

    public void a(NBTTagCompound nbttagcompound) {
        super.a(nbttagcompound);
        this.datawatcher.watch(17, Byte.valueOf((byte) (nbttagcompound.getBoolean("powered") ? 1 : 0)));
    }

    protected void b(Entity entity, float f) {
        if (!this.world.isStatic) {
            if (this.fuseTicks > 0) {
                this.b(-1);
                --this.fuseTicks;
                if (this.fuseTicks < 0) {
                    this.fuseTicks = 0;
                }
            }
        }
    }

    public void w_() {
        this.b = this.fuseTicks;
        if (this.world.isStatic) {
            int i = this.A();

            if (i > 0 && this.fuseTicks == 0) {
                this.world.makeSound(this, "random.fuse", 1.0F, 0.5F);
            }

            this.fuseTicks += i;
            if (this.fuseTicks < 0) {
                this.fuseTicks = 0;
            }

            if (this.fuseTicks >= 30) {
                this.fuseTicks = 30;
            }
        }

        super.w_();
        if (this.target == null && this.fuseTicks > 0) {
            this.b(-1);
            --this.fuseTicks;
            if (this.fuseTicks < 0) {
                this.fuseTicks = 0;
            }
        }
    }

    protected String m() {
        return "mob.creeper";
    }

    protected String n() {
        return "mob.creeperdeath";
    }

    public void die(DamageSource damagesource) {
        super.die(damagesource);
        if (damagesource.getEntity() instanceof EntitySkeleton) {
            this.b(Item.RECORD_1.id + this.random.nextInt(2), 1);
        }
    }

    protected void a(Entity entity, float f) {
        if (!this.world.isStatic) {
            int i = this.A();

            if ((i > 0 || f >= 3.0F) && (i <= 0 || f >= 7.0F)) {
                this.b(-1);
                --this.fuseTicks;
                if (this.fuseTicks < 0) {
                    this.fuseTicks = 0;
                }
            } else {
                if (this.fuseTicks == 0) {
                    this.world.makeSound(this, "random.fuse", 1.0F, 0.5F);
                }

                this.b(1);
                ++this.fuseTicks;
                if (this.fuseTicks >= 30) {
                    // CraftBukkit start
                    float radius = this.isPowered() ? 6.0F : 3.0F;

                    ExplosionPrimeEvent event = new ExplosionPrimeEvent(CraftEntity.getEntity(this.world.getServer(), this), radius, false);
                    this.world.getServer().getPluginManager().callEvent(event);

                    if (!event.isCancelled()) {
                        this.world.createExplosion(this, this.locX, this.locY, this.locZ, event.getRadius(), event.getFire());
                        this.die();
                    } else {
                        this.fuseTicks = 0;
                    }
                    // CraftBukkit end
                }

                this.e = true;
            }
        }
    }

    public boolean isPowered() {
        return this.datawatcher.getByte(17) == 1;
    }

    protected int e() {
        return Item.SULPHUR.id;
    }

    private int A() {
        return this.datawatcher.getByte(16);
    }

    private void b(int i) {
        this.datawatcher.watch(16, Byte.valueOf((byte) i));
    }

    public void a(EntityWeatherLighting entityweatherlighting) {
        super.a(entityweatherlighting);

        // CraftBukkit start
        CreeperPowerEvent event = new CreeperPowerEvent(this.getBukkitEntity(), entityweatherlighting.getBukkitEntity(), CreeperPowerEvent.PowerCause.LIGHTNING);
        this.world.getServer().getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            return;
        }

        this.setPowered(true);
    }

    public void setPowered(boolean powered) {
        if (!powered) {
            this.datawatcher.watch(17, Byte.valueOf((byte) 0));
        } else
        // CraftBukkit end
        this.datawatcher.watch(17, Byte.valueOf((byte) 1));
    }
}
