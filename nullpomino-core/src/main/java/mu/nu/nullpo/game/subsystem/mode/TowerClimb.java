package mu.nu.nullpo.game.subsystem.mode;

import java.util.Random;
import java.util.ArrayList;
import java.util.List;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.CompletionStage;

import mu.nu.nullpo.game.component.BGMStatus;
import mu.nu.nullpo.game.component.Controller;
import mu.nu.nullpo.game.event.EventReceiver;
import mu.nu.nullpo.game.play.GameEngine;
import mu.nu.nullpo.util.CustomProperties;
import mu.nu.nullpo.util.GeneralUtil;
import mu.nu.nullpo.game.component.Block;
import mu.nu.nullpo.game.component.Piece;

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

    private static final float[] tableFloors = {0, 50, 150, 300, 450, 650, 850, 1100, 1350, 1650, Float.POSITIVE_INFINITY};

    // Game Stats
    private int kos;

    private int bgmlv;

    private int garbage;
    private int garbageSent;
    private List<Integer> garbageEntries;
    private int lastHole;

    private int lastEvent;
    private int lastCombo;
    private int lastPiece;

    private int b2b;

    // CLimbing
    private float altitude;
    private int speed_rank;
    private float speed_exp;
    private int speed_rank_locked_until;
    private boolean last_rank_change_was_promote;
    private int promotion_fatigue;

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
        speed_rank = 1;
        speed_exp = 0;
        kos = 0;
        b2b = 0;
        lastEvent = EVENT_NONE;
        last_rank_change_was_promote = false;
        promotion_fatigue = 0;
        speed_rank_locked_until = 0;
        garbageEntries = new ArrayList<>();
        lastHole = engine.random.nextInt(engine.field.getWidth());

        if (owner.replayMode) {
            loadSetting(owner.replayProp);
        } else {
            version = CURRENT_VERSION;
        }
    }

    protected void loadSetting(CustomProperties prop) {}

    protected void saveSetting(CustomProperties prop) {}
    
    private void sendAttack(GameEngine engine, int amount) {}
        
    private void recieveAttack(GameEngine engine, int amount) {
        garbageEntries.add(amount);
    }

    private void garbagerising(GameEngine engine, int lines) {}

    private void getTotalAmount(GameEngine engine) {}

    private void setStartBgmlv(GameEngine engine) {
        bgmlv = 0;
    }

    private void setSpeed(GameEngine engine) {
        engine.speed.are = 0;
        engine.speed.areLine = 0;
        if (mod_ms == 2) {
            engine.speed.lineDelay = 70;
        } else {
            engine.speed.lineDelay = 0;
        }
    }

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
        engine.field.addHurryupFloor(10, engine.getSkin());
        setSpeed(engine);
    }

    @Override
    public void renderLast(GameEngine engine, int playerID) {
        if (owner.menuOnly) return;

        receiver.drawScoreFont(engine, playerID, 0, 0, "TOWER CLIMBER");
        receiver.drawScoreFont(engine, playerID, 0, 1, "MODE IS WIP");

        if ((engine.stat == GameEngine.Status.SETTING) || ((engine.stat == GameEngine.Status.RESULT) && (owner.replayMode == false))) {}
        else {
            int time = engine.statistics.time;
            receiver.drawScoreFont(engine, playerID, 0, 3, "KO'S", EventReceiver.COLOR_BLUE);
            receiver.drawScoreFont(engine, playerID, 0, 4, String.valueOf(kos));
            
            receiver.drawScoreFont(engine, playerID, 0, 6, "PPS", EventReceiver.COLOR_BLUE);
            receiver.drawScoreFont(engine, playerID, 0, 4, String.valueOf(engine.statistics.pps));
            
            receiver.drawScoreFont(engine, playerID, 0, 9, "ATTACK", EventReceiver.COLOR_BLUE);
            receiver.drawScoreFont(engine, playerID, 0, 10, String.format("%d, %.2f/M", garbageSent, (float)(garbageSent * 3600) / (float)(time)));
            
            
            receiver.drawScoreFont(engine, playerID, 0, 12, "TIME", EventReceiver.COLOR_BLUE);
            receiver.drawScoreFont(engine, playerID, 0, 13, GeneralUtil.getTime(time));
            
            receiver.drawScoreFont(engine, playerID, 0, 15, "B2B", EventReceiver.COLOR_BLUE);
            receiver.drawScoreFont(engine, playerID, 0, 16, String.valueOf(b2b));
            
            receiver.drawScoreFont(engine, playerID, 0, 18, "ALTITUDE", EventReceiver.COLOR_BLUE);
            receiver.drawScoreFont(engine, playerID, 0, 19, String.valueOf(altitude));

            String rank_str = String.format("%3d", speed_rank);
            receiver.drawScoreFont(engine, playerID, 0, 21, rank_str);
            String exp_str = String.format("%.1f/%d", speed_exp, 4 * speed_rank);

            if((lastEvent != EVENT_NONE)) { // && (scgettime < 120)
                String strPieceName = Piece.getPieceName(lastPiece);
            
                switch(lastEvent) {
                case EVENT_SINGLE:
                    receiver.drawMenuFont(engine, playerID, 2, 21, "SINGLE", EventReceiver.COLOR_DARKBLUE);
                    break;
                case EVENT_DOUBLE:
                    receiver.drawMenuFont(engine, playerID, 2, 21, "DOUBLE", EventReceiver.COLOR_BLUE);
                    break;
                case EVENT_TRIPLE:
                    receiver.drawMenuFont(engine, playerID, 2, 21, "TRIPLE", EventReceiver.COLOR_GREEN);
                    break;
                case EVENT_FOUR:
                    if(b2b > 0) receiver.drawMenuFont(engine, playerID, 3, 21, "FOUR", EventReceiver.COLOR_RED);
                    else receiver.drawMenuFont(engine, playerID, 3, 21, "FOUR", EventReceiver.COLOR_ORANGE);
                    break;
                case EVENT_TSPIN_SINGLE_MINI:
                    if(b2b > 0) receiver.drawMenuFont(engine, playerID, 1, 21, strPieceName + "-MINI-S", EventReceiver.COLOR_RED);
                    else receiver.drawMenuFont(engine, playerID, 1, 21, strPieceName + "-MINI-S", EventReceiver.COLOR_ORANGE);
                    break;
                case EVENT_TSPIN_SINGLE:
                    if(b2b > 0) receiver.drawMenuFont(engine, playerID, 1, 21, strPieceName + "-SINGLE", EventReceiver.COLOR_RED);
                    else receiver.drawMenuFont(engine, playerID, 1, 21, strPieceName + "-SINGLE", EventReceiver.COLOR_ORANGE);
                    break;
                case EVENT_TSPIN_DOUBLE_MINI:
                    if(b2b > 0) receiver.drawMenuFont(engine, playerID, 1, 21, strPieceName + "-MINI-D", EventReceiver.COLOR_RED);
                    else receiver.drawMenuFont(engine, playerID, 1, 21, strPieceName + "-MINI-D", EventReceiver.COLOR_ORANGE);
                    break;
                case EVENT_TSPIN_DOUBLE:
                    if(b2b > 0) receiver.drawMenuFont(engine, playerID, 1, 21, strPieceName + "-DOUBLE", EventReceiver.COLOR_RED);
                    else receiver.drawMenuFont(engine, playerID, 1, 21, strPieceName + "-DOUBLE", EventReceiver.COLOR_ORANGE);
                    break;
                case EVENT_TSPIN_TRIPLE:
                    if(b2b > 0) receiver.drawMenuFont(engine, playerID, 1, 21, strPieceName + "-TRIPLE", EventReceiver.COLOR_RED);
                    else receiver.drawMenuFont(engine, playerID, 1, 21, strPieceName + "-TRIPLE", EventReceiver.COLOR_ORANGE);
                    break;
                case EVENT_TSPIN_EZ:
                    if(b2b > 0) receiver.drawMenuFont(engine, playerID, 3, 21, "EZ-" + strPieceName, EventReceiver.COLOR_RED);
                    else receiver.drawMenuFont(engine, playerID, 3, 21, "EZ-" + strPieceName, EventReceiver.COLOR_ORANGE);
                    break;
                }
            
                if(lastCombo >= 2) {
                    receiver.drawMenuFont(engine, playerID, 2, 22, (lastCombo - 1) + "COMBO", EventReceiver.COLOR_CYAN);
                }

                if(garbage > 0) {
                    int x = receiver.getFieldDisplayPositionX(engine, playerID);
                    int y = receiver.getFieldDisplayPositionY(engine, playerID);
                    int fontColor = EventReceiver.COLOR_WHITE;

                    if (garbage >= 1) fontColor = EventReceiver.COLOR_YELLOW;
                    if (garbage >= 3) fontColor = EventReceiver.COLOR_ORANGE;
                    if (garbage >= 4) fontColor = EventReceiver.COLOR_RED;

                    String strTempGarbage = String.format("%5d", garbage);
                    receiver.drawDirectFont(engine, playerID, x + 96, y + 372, strTempGarbage, fontColor);
                }
            }
        }
    }


    @Override
    public boolean onMove(GameEngine engine, int playerID) {
        return super.onMove(engine, playerID);
    }

    @Override
    public void onLast(GameEngine engine, int playerID) {
        int time = engine.statistics.time; // Note: game running on 60hz
        int rank = speed_rank;
        
        // climbing related
        float nextRankXP = 4 * rank;
        float storedXP = 4 * (rank - 1);
        if (speed_exp < 0) {
            if (rank <= 1) {
                speed_exp = 0;
            } else {
                speed_exp += storedXP;
                last_rank_change_was_promote = false;
                rank--;
            }
        } else if (speed_exp >= nextRankXP) {
            speed_exp -= nextRankXP;
            last_rank_change_was_promote = true;
            speed_rank_locked_until = time + Math.max(60, 60 * (5 - promotion_fatigue));
            promotion_fatigue++;
            rank++;
        }

        if (last_rank_change_was_promote && speed_exp >= 2 * (rank - 1)) {
            promotion_fatigue = 0;
        }

        speed_rank = Math.round(rank + speed_exp / (4 * rank));
    }

    @Override 
    public void calcScore(GameEngine engine, int playerID, int lines) {
        // Attack
        if (lines > 0) {
            int pts = 0;

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
                    if (mod_ex == 0 && (engine.combo - 1 <= 0)) {
                        pts += 1;
                    }
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
                int b2b_surge = b2b;
                if (b2b < 4) {
                    b2b_surge = 0;
                } else if (mod_as != 2) {
                    b2b_surge = Math.max(0, b2b_surge - 3);
                }
                pts += b2b_surge;
                b2b = 0;
            }

            // Combo
            if (engine.comboType != GameEngine.COMBO_TYPE_DISABLE) {
                int cmbindex = engine.combo - 1;
                if (cmbindex < 0) cmbindex = 0;
                if (cmbindex >= COMBO_ATTACK_TABLE.length) cmbindex = COMBO_ATTACK_TABLE.length - 1;
                pts += COMBO_ATTACK_TABLE[cmbindex];
                lastCombo = engine.combo;
            }

            // All Clear
            if ((lines >= 1) && (engine.field.isEmpty())) {
                engine.playSE("bravo");
                pts += 3;
                b2b += 1;
            }

            // Attack lines count
            garbageSent += pts;
            lastPiece = engine.nowPieceObject.id;

            // Cancelling
            int original_attack = pts;
            if (mod_dh != 2 && garbage > 0) {
                garbage -= pts;
                if (garbage < 0) {
                    pts = Math.abs(garbage);
                    garbage = 0;
                } else {
                    pts = 0;
                }
            }

            // [TODO] send attack
            if (pts > 0) {
                // garbage += pts;
            }
        } 

        if (lines <= 0) {
            if (garbage > 0) {
                engine.field.addSingleHoleGarbage(engine.random.nextInt(engine.field.getWidth()), PLAYER_COLOR_BLOCK, engine.getSkin(), garbage);
                garbage = 0;
            }
            if (mod_as == 0) {
                lastEvent = EVENT_NONE;
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
