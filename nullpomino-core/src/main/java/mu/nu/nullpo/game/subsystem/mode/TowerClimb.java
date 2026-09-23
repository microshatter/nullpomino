package mu.nu.nullpo.game.subsystem.mode;

import java.util.Random;

import mu.nu.nullpo.game.component.BGMStatus;
import mu.nu.nullpo.game.component.Controller;
import mu.nu.nullpo.game.event.EventReceiver;
import mu.nu.nullpo.game.play.GameEngine;
import mu.nu.nullpo.util.CustomProperties;
import mu.nu.nullpo.util.GeneralUtil;
import mu.nu.nullpo.game.component.Block;

import org.apache.log4j.Logger;

public class TowerClimb extends AbstractMode {
    static final Logger log = Logger.getLogger(TowerClimb.class);

    private static final int CURRENT_VERSION = 1;

    // Game Settings
    private static final int EVENT_NONE = 0,
                             EVENT_SINGLE = 1,
                             EVENT_DOUBLE = 2,
                             EVENT_TRIPLE = 3,
                             EVENT_FOUR = 4,
                             EVENT_TSPIN_SINGLE_MINI = 5,
                             EVENT_TSPIN_SINGLE = 6,
                             EVENT_TSPIN_DOUBLE = 7,
                             EVENT_TSPIN_TRIPLE = 8,
                             EVENT_TSPIN_DOUBLE_MINI = 9,
                             EVENT_TSPIN_EZ = 10,
                             EVENT_TSPIN_TRIPLE_MINI = 11;

    private final int[] COMBO_ATTACK_TABLE = {0,0,1,1,2,2,3,3,4,4,4,5};

    private final int PLAYER_COLOR_BLOCK = Block.BLOCK_COLOR_GRAY; // Garbage Block

    private static final float[] tableFloors = {0, 50, 150, 300, 450, 650, 850, 1100, 1350, 1650};

    // Game Stats
    private float altitude;

    private int kos;

    private int bgmlv;

    private int garbage;
    private int garbageSent;

    private int lastEvent;

    private int b2b;


    // Mods
    // 0 = disabled, 1 = enabled, 2 = reverse
    private int mod_ex; // Expert
    private int mod_nh; // No Hold
    private int mod_ms; // Messiness
    private int mod_gv; // Gravity
    private int mod_vl; // Volatility
    private int mod_dh; // Double Hole
    private int mod_in; // Invisible
    private int mod_as; // All Spin
    private int mod_dp; // Duo, Unsupported

    private int version;

    @Override
    public String getName() {
        return "TOWER CLIMB";
    }

    @Override
    public void playerInit(GameEngine engine, int playerID) {
        // Base Engine loader
        owner = engine.owner;
        receiver = engine.owner.receiver;

        garbageSent = 0;
        garbage = 0;
        altitude = 0.0f;
        kos = 0;

        if (owner.replayMode) {
            loadSetting(owner.replayProp);
        } else {
            version = CURRENT_VERSION;
        }
    }

    protected void loadSetting(CustomProperties prop) {}

    protected void saveSetting(CustomProperties prop) {}

    private void setStartBgmlv(GameEngine engine) {
        bgmlv = 0;
    }

    private void setSpeed(GameEngine engine) {}

    @Override
    public void renderSetting(GameEngine engine, int playerID) {}

    @Override
    public boolean onReady(GameEngine engine, int playerID) {
        return false;
    }

    @Override
    public void startGame(GameEngine engine, int playerID) {
        engine.comboType = GameEngine.COMBO_TYPE_NORMAL;
        engine.tspinEnable = true;
        engine.useAllSpinBonus = true;
        engine.b2bEnable = true;
    }

    @Override
    public void renderLast(GameEngine engine, int playerID) {
        if (owner.menuOnly) return;

        receiver.drawScoreFont(engine, playerID, 0, 0, "TOWER CLIMBER");
        receiver.drawScoreFont(engine, playerID, 0, 1, "MODE IS WIP");

        if ((engine.stat == GameEngine.Status.SETTING) || ((engine.stat == GameEngine.Status.RESULT) && (owner.replayMode == false))) {}
        else {
            receiver.drawScoreFont(engine, playerID, 0, 3, "KO'S", EventReceiver.COLOR_BLUE);
            receiver.drawScoreFont(engine, playerID, 0, 4, String.valueOf(kos));
            
            receiver.drawScoreFont(engine, playerID, 0, 6, "PPS", EventReceiver.COLOR_BLUE);
            
            receiver.drawScoreFont(engine, playerID, 0, 9, "ATTACK", EventReceiver.COLOR_BLUE);
            receiver.drawScoreFont(engine, playerID, 0, 13, String.valueOf(garbageSent));
            
            receiver.drawScoreFont(engine, playerID, 0, 12, "TIME", EventReceiver.COLOR_BLUE);
            int time = engine.statistics.time;
            receiver.drawScoreFont(engine, playerID, 0, 13, GeneralUtil.getTime(time));
            
            receiver.drawScoreFont(engine, playerID, 0, 15, "B2B", EventReceiver.COLOR_BLUE);
            receiver.drawScoreFont(engine, playerID, 0, 16, String.valueOf(b2b));
            
        }
    }


    @Override
    public boolean onMove(GameEngine engine, int playerID) {
        return super.onMove(engine, playerID);
    }

    @Override
    public void onLast(GameEngine engine, int playerID) {}

    @Override 
    public void calcScore(GameEngine engine, int playerID, int lines) {
        // Attack
        if (lines > 0) {
            int pts = 0;
            int ptsB2B = 0;

            if (engine.tspin) {
                if(engine.tspinez) {
                    lastEvent = EVENT_TSPIN_EZ;
                } else if (lines == 1) {
                    if (engine.tspinmini) {
                        lastEvent = EVENT_TSPIN_SINGLE_MINI;
                    } else {
                        pts += 2;
                        lastEvent = EVENT_TSPIN_SINGLE;
                    }
                } else if (lines == 2) {
                    if (engine.tspinmini) {
                        lastEvent = EVENT_TSPIN_DOUBLE_MINI;
                    } else {
                        pts += 4;
                        lastEvent = EVENT_TSPIN_DOUBLE;
                    }
                } else if (lines == 3) {
                    if (engine.tspinmini) {
                        lastEvent = EVENT_TSPIN_TRIPLE_MINI;
                    } else {
                        pts += 6;
                        lastEvent = EVENT_TSPIN_TRIPLE;
                    }
                }
            } else {
                if (lines == 1) {
                    lastEvent = EVENT_SINGLE;
                } else if (lines == 2) {
                    pts += 1;
                    lastEvent = EVENT_DOUBLE;
                } else if (lines == 3) {
                    pts += 2;
                    lastEvent = EVENT_TRIPLE;
                } else if (lines == 4) {
                    pts += 4;
                    lastEvent = EVENT_FOUR;
                }
            }

            // B2B
            if (engine.b2b) {
                b2b += 1;
                pts += 1;
            } else {
                pts += b2b;
                b2b = 0;
            }
        } 
    }

    @Override
    public boolean onGameOver(GameEngine engine, int playerID) {
        return false;
    }

    @Override
    public void renderResult(GameEngine engine, int playerID) {
        drawResult(engine, playerID, receiver, 0, EventReceiver.COLOR_BLUE, "MODE IS WIP");
        drawResult(engine, playerID, receiver, 1, EventReceiver.COLOR_BLUE, "REPLAY IS UNAVAILABLE");
    }

    @Override
    public void saveReplay(GameEngine engine, int playerID, CustomProperties prop) {}
}
