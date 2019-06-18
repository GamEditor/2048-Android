package com.gameditors.a2048;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainGame
{
    public static final int SPAWN_ANIMATION = -1;
    public static final int MOVE_ANIMATION = 0;
    public static final int MERGE_ANIMATION = 1;

    public static final int FADE_GLOBAL_ANIMATION = 0;
    private static final long MOVE_ANIMATION_TIME = MainView.BASE_ANIMATION_TIME;
    private static final long SPAWN_ANIMATION_TIME = MainView.BASE_ANIMATION_TIME;
    private static final long NOTIFICATION_DELAY_TIME = MOVE_ANIMATION_TIME + SPAWN_ANIMATION_TIME;
    private static final long NOTIFICATION_ANIMATION_TIME = MainView.BASE_ANIMATION_TIME * 5;
    private static final int startingMaxValue = 2048;
    //Odd state = game is not active
    //Even state = game is active
    //Win state = active state + 1
    private static final int GAME_WIN = 1;
    private static final int GAME_LOST = -1;
    private static final int GAME_NORMAL = 0;
    public int gameState = GAME_NORMAL;
    public int lastGameState = GAME_NORMAL;
    private int bufferGameState = GAME_NORMAL;
    private static final int GAME_ENDLESS = 2;
    private static final int GAME_ENDLESS_WON = 3;
    private static final String HIGH_SCORE = "high score";
    private static int endingMaxValue;
    private final Context mContext;
    private final MainView mView;
    public Grid grid = null;
    public AnimationGrid aGrid;
    public boolean canUndo;
    public long score = 0;
    public long highScore = 0;
    public long lastScore = 0;
    private long bufferScore = 0;

    public MainGame(Context context, MainView view)
    {
        mContext = context;
        mView = view;
        endingMaxValue = (int) Math.pow(2, view.numCellTypes - 1);
    }

    public void newGame()
    {
        final int rows = MainMenuActivity.getRows();
        if (grid == null)
            grid = new Grid(rows, rows);
        else
        {
            prepareUndoState();
            saveUndoState();
            grid.clearGrid();
        }

        aGrid = new AnimationGrid(rows, rows);
        highScore = getHighScore();
        if (score >= highScore)
        {
            highScore = score;
            recordHighScore();
        }
        score = 0;
        gameState = GAME_NORMAL;
        addStartTiles();
        mView.refreshLastTime = true;
        mView.resyncTime();
        mView.invalidate();
    }

    private void addStartTiles()
    {
        int startTiles = 2;
        for (int xx = 0; xx < startTiles; xx++)
            this.addRandomTile();
    }

    private void addRandomTile()
    {
        if (grid.isCellsAvailable())
        {
            int value = Math.random() < 0.9 ? 2 : 4;
            Tile tile = new Tile(grid.randomAvailableCell(), value);
            spawnTile(tile);
        }
    }

    private void spawnTile(Tile tile)
    {
        grid.insertTile(tile);
        aGrid.startAnimation(tile.getX(), tile.getY(), SPAWN_ANIMATION,
                SPAWN_ANIMATION_TIME, MOVE_ANIMATION_TIME, null); //Direction: -1 = EXPANDING
    }

    private void recordHighScore()
    {
        final int rows = MainMenuActivity.getRows();
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mContext);
        SharedPreferences.Editor editor = settings.edit();
        editor.putLong(HIGH_SCORE + rows, highScore);
        editor.apply();
    }

    private long getHighScore()
    {
        final int rows = MainMenuActivity.getRows();
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mContext);
        return settings.getLong(HIGH_SCORE + rows, -1);
    }

    private void prepareTiles()
    {
        for (Tile[] array : grid.field)
            for (Tile tile : array)
                if (grid.isCellOccupied(tile))
                    tile.setMergedFrom(null);
    }

    private void moveTile(Tile tile, Cell cell)
    {
        grid.field[tile.getX()][tile.getY()] = null;
        grid.field[cell.getX()][cell.getY()] = tile;
        tile.updatePosition(cell);
    }

    private void saveUndoState()
    {
        grid.saveTiles();
        canUndo = true;
        lastScore = bufferScore;
        lastGameState = bufferGameState;
    }

    private void prepareUndoState()
    {
        grid.prepareSaveTiles();
        bufferScore = score;
        bufferGameState = gameState;
    }

    public void revertUndoState()
    {
        if (canUndo)
        {
            canUndo = false;
            aGrid.cancelAnimations();
            grid.revertTiles();
            score = lastScore;
            gameState = lastGameState;
            mView.refreshLastTime = true;
            mView.invalidate();
        }
    }

    public boolean gameWon()
    {
        return (gameState > 0 && gameState % 2 != 0);
    }

    public boolean gameLost()
    {
        return (gameState == GAME_LOST);
    }

    public boolean isActive()
    {
        return !(gameWon() || gameLost());
    }

    public void move(int direction)
    {
        aGrid.cancelAnimations();
        // 0: up, 1: right, 2: down, 3: left
        if (!isActive())
            return;

        prepareUndoState();
        Cell vector = getVector(direction);
        List<Integer> traversalsX = buildTraversalsX(vector);
        List<Integer> traversalsY = buildTraversalsY(vector);
        boolean moved = false;

        prepareTiles();

        for (int xx : traversalsX)
        {
            for (int yy : traversalsY)
            {
                Cell cell = new Cell(xx, yy);
                Tile tile = grid.getCellContent(cell);

                if (tile != null)
                {
                    Cell[] positions = findFarthestPosition(cell, vector);
                    Tile next = grid.getCellContent(positions[1]);

                    if (next != null && next.getValue() == tile.getValue() && next.getMergedFrom() == null)
                    {
                        Tile merged = new Tile(positions[1], tile.getValue() * 2);
                        Tile[] temp = {tile, next};
                        merged.setMergedFrom(temp);

                        grid.insertTile(merged);
                        grid.removeTile(tile);

                        // Converge the two tiles' positions
                        tile.updatePosition(positions[1]);

                        int[] extras = {xx, yy};
                        aGrid.startAnimation(merged.getX(), merged.getY(), MOVE_ANIMATION,
                                MOVE_ANIMATION_TIME, 0, extras); //Direction: 0 = MOVING MERGED
                        aGrid.startAnimation(merged.getX(), merged.getY(), MERGE_ANIMATION,
                                SPAWN_ANIMATION_TIME, MOVE_ANIMATION_TIME, null);

                        // Update the score
                        score = score + merged.getValue();
                        highScore = Math.max(score, highScore);

                        // The mighty 2048 tile
                        if (merged.getValue() >= winValue() && !gameWon())
                        {
                            gameState = gameState + GAME_WIN; // Set win state
                            endGame();
                        }

                        if(!MainMenuActivity.mIsMainMenu)
                        {
                            if(merged.getValue() >= 32)
                            {
                                MainActivity.unlockAchievement(merged.getValue());
                                mView.mActivity.pushAccomplishments();
                            }
                        }
                    }
                    else
                    {
                        moveTile(tile, positions[0]);
                        int[] extras = {xx, yy, 0};
                        aGrid.startAnimation(positions[0].getX(), positions[0].getY(), MOVE_ANIMATION, MOVE_ANIMATION_TIME, 0, extras); //Direction: 1 = MOVING NO MERGE
                    }

                    if (!positionsEqual(cell, tile))
                        moved = true;
                }
            }
        }

        if (moved)
        {
            saveUndoState();
            addRandomTile();
            checkLose();
        }
        mView.resyncTime();
        mView.invalidate();
    }

    private void checkLose()
    {
        if (!movesAvailable() && !gameWon())
        {
            gameState = GAME_LOST;
            endGame();
        }
    }

    private void endGame()
    {
        aGrid.startAnimation(-1, -1, FADE_GLOBAL_ANIMATION, NOTIFICATION_ANIMATION_TIME, NOTIFICATION_DELAY_TIME, null);
        if (score >= highScore)
        {
            highScore = score;
            recordHighScore();
        }
    }

    private Cell getVector(int direction)
    {
        Cell[] map = {
            new Cell(0, -1), // up
            new Cell(1, 0),  // right
            new Cell(0, 1),  // down
            new Cell(-1, 0)  // left
        };
        return map[direction];
    }

    private List<Integer> buildTraversalsX(Cell vector)
    {
        List<Integer> traversals = new ArrayList<>();

        final int rows = MainMenuActivity.getRows();
        for (int xx = 0; xx < rows; xx++)
            traversals.add(xx);

        if (vector.getX() == 1)
            Collections.reverse(traversals);

        return traversals;
    }

    private List<Integer> buildTraversalsY(Cell vector)
    {
        List<Integer> traversals = new ArrayList<>();

        final int rows = MainMenuActivity.getRows();
        for (int xx = 0; xx < rows; xx++)
            traversals.add(xx);

        if (vector.getY() == 1)
            Collections.reverse(traversals);

        return traversals;
    }

    private Cell[] findFarthestPosition(Cell cell, Cell vector)
    {
        Cell previous;
        Cell nextCell = new Cell(cell.getX(), cell.getY());
        do
        {
            previous = nextCell;
            nextCell = new Cell(previous.getX() + vector.getX(),
                    previous.getY() + vector.getY());
        } while (grid.isCellWithinBounds(nextCell) && grid.isCellAvailable(nextCell));

        return new Cell[]{previous, nextCell};
    }

    private boolean movesAvailable()
    {
        return grid.isCellsAvailable() || tileMatchesAvailable();
    }

    private boolean tileMatchesAvailable()
    {
        Tile tile;

        final int rows = MainMenuActivity.getRows();
        for (int xx = 0; xx < rows; xx++)
        {
            for (int yy = 0; yy < rows; yy++)
            {
                tile = grid.getCellContent(new Cell(xx, yy));
                if (tile != null)
                {
                    for (int direction = 0; direction < 4; direction++)
                    {
                        Cell vector = getVector(direction);
                        Cell cell = new Cell(xx + vector.getX(), yy + vector.getY());

                        Tile other = grid.getCellContent(cell);

                        if (other != null && other.getValue() == tile.getValue())
                            return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean positionsEqual(Cell first, Cell second)
    {
        return first.getX() == second.getX() && first.getY() == second.getY();
    }

    private int winValue()
    {
        if (!canContinue())
            return endingMaxValue;
        else
            return startingMaxValue;
    }

    public void setEndlessMode()
    {
        gameState = GAME_ENDLESS;
        mView.invalidate();
        mView.refreshLastTime = true;
    }

    public boolean canContinue()
    {
        return !(gameState == GAME_ENDLESS || gameState == GAME_ENDLESS_WON);
    }

    private void customSaveLoadTemp()
    {
        final int rows = MainMenuActivity.getRows();

        final String WIDTH = "width" + rows + "temp";
        final String HEIGHT = "height" + rows + "temp";

        int deleteAmount = rows - 1;

        // Save() as "temp"
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mContext);
        SharedPreferences.Editor editor = settings.edit();
        Tile[][] field = grid.field;
        editor.putInt(WIDTH, field.length);
        editor.putInt(HEIGHT, field.length);

        for (int xx = 0; xx < field.length; xx++)
        {
            for (int yy = 0; yy < field[0].length; yy++)
            {
                if (field[xx][yy] != null)
                {
                    if (field[xx][yy].getValue() >= 2 && field[xx][yy].getValue() <= 32 && deleteAmount > 0)
                    {
                        deleteAmount--;
                        editor.putInt(rows + " " + xx + " " + yy + "temp", 0);
                    }
                    else
                        editor.putInt(rows + " " + xx + " " + yy + "temp", field[xx][yy].getValue());
                }
                else
                    editor.putInt(rows + " " + xx + " " + yy + "temp", 0);
            }
        }
        editor.apply();

        // Load() as "temp"
        for (int xx = 0; xx < grid.field.length; xx++)
        {
            for (int yy = 0; yy < grid.field[0].length; yy++)
            {
                int value = settings.getInt( rows + " " + xx + " " + yy + "temp", -1);
                if (value > 0)
                    grid.field[xx][yy] = new Tile(xx, yy, value);
                else if (value == 0)
                    grid.field[xx][yy] = null;
            }
        }

        canUndo = false;
        gameState = lastGameState;
    }

    public void makeToast(int resId)
    {
        Toast.makeText(mContext, mContext.getString(resId), Toast.LENGTH_SHORT).show();
    }

    public void RemoveTilesWithTrash()
    {
        final int rows = MainMenuActivity.getRows();
        int cellCount = (rows * rows) - (rows + 2);

        if(mContext.getClass() == ColorPickerActivity.class)    // because of color picker
        {
            if(mView.game.grid.getAvailableCells().size() < cellCount)
            {
                customSaveLoadTemp();
                mView.invalidate();
            }
            else
                mView.game.makeToast(R.string.tiles_are_not_enough_to_remove);
        }
        else
        {
            if(MainActivity.mRewardDeletes > 0)
            {
                if(mView.game.grid.getAvailableCells().size() < cellCount)
                {
                    new AlertDialog.Builder(mView.getContext())
                            .setPositiveButton(R.string.yes_delete_tiles, new DialogInterface.OnClickListener()
                            {
                                @Override
                                public void onClick(DialogInterface dialog, int which)
                                {
                                    MainActivity.mRewardDeletes--;  // decrease rewards
                                    customSaveLoadTemp();
                                    mView.invalidate();
                                }
                            })
                            .setNegativeButton(R.string.dont_delete_tiles, null)
                            .setTitle(R.string.trash_dialog_title)
                            .setMessage(R.string.trash_dialog_message)
                            .show();
                }
                else
                    mView.game.makeToast(R.string.tiles_are_not_enough_to_remove);
            }
            else
                mView.game.makeToast(R.string.reward_amount_error);
        }
    }

    public void loadCurrentBoard()
    {
        //Stopping all animations
        mView.game.aGrid.cancelAnimations();

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mContext);

        final int rows = MainMenuActivity.getRows();

        if(!settings.getBoolean("has_snapshot" + rows, false))
        {
            // if we haven't any snapshot already, so we better don't loading anything
            mView.game.makeToast(R.string.loading_failed);
            return;
        }

        final String CURRENT_STATE = "cs";

        final String UNDO_GRID = "undo" + rows + CURRENT_STATE;
        final String REWARD_DELETES = "reward chances" + rows + CURRENT_STATE;
        final String SCORE = "score" + rows + CURRENT_STATE;
        final String UNDO_SCORE = "undo score" + rows + CURRENT_STATE;
        final String CAN_UNDO = "can undo" + rows + CURRENT_STATE;
        final String GAME_STATE = "game state" + rows + CURRENT_STATE;
        final String UNDO_GAME_STATE = "undo game state" + rows + CURRENT_STATE;

        for (int xx = 0; xx < mView.game.grid.field.length; xx++)
        {
            for (int yy = 0; yy < mView.game.grid.field[0].length; yy++)
            {
                int value = settings.getInt( CURRENT_STATE + rows + " " + xx + " " + yy, -1);
                if (value > 0)
                    mView.game.grid.field[xx][yy] = new Tile(xx, yy, value);
                else if (value == 0)
                    mView.game.grid.field[xx][yy] = null;

                int undoValue = settings.getInt(UNDO_GRID + rows + " " + xx + " " + yy, -1);
                if (undoValue > 0)
                    mView.game.grid.undoField[xx][yy] = new Tile(xx, yy, undoValue);
                else if (value == 0)
                    mView.game.grid.undoField[xx][yy] = null;
            }
        }

        MainActivity.mRewardDeletes = settings.getInt(REWARD_DELETES, 2);

        mView.game.score = settings.getLong(SCORE, mView.game.score);
        mView.game.highScore = settings.getLong(HIGH_SCORE, mView.game.highScore);
        mView.game.lastScore = settings.getLong(UNDO_SCORE, mView.game.lastScore);
        mView.game.canUndo = settings.getBoolean(CAN_UNDO, mView.game.canUndo);
        mView.game.gameState = settings.getInt(GAME_STATE, mView.game.gameState);
        mView.game.lastGameState = settings.getInt(UNDO_GAME_STATE, mView.game.lastGameState);

        mView.invalidate();
        mView.game.makeToast(R.string.loaded);
    }

    public void saveCurrentBoard()
    {
        if(!mView.game.isActive())
        {
            mView.game.makeToast(R.string.message_unable_saving_on_game_over);
            return;
        }

        final int rows = MainMenuActivity.getRows();

        final String CURRENT_STATE = "cs";

        final String WIDTH = "width" + rows + CURRENT_STATE;
        final String HEIGHT = "height" + rows + CURRENT_STATE;
        final String UNDO_GRID = "undo" + rows + CURRENT_STATE;
        final String REWARD_DELETES = "reward chances" + rows + CURRENT_STATE;
        final String SCORE = "score" + rows + CURRENT_STATE;
        final String UNDO_SCORE = "undo score" + rows + CURRENT_STATE;
        final String CAN_UNDO = "can undo" + rows + CURRENT_STATE;
        final String GAME_STATE = "game state" + rows + CURRENT_STATE;
        final String UNDO_GAME_STATE = "undo game state" + rows + CURRENT_STATE;

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mContext);
        SharedPreferences.Editor editor = settings.edit();
        Tile[][] field = mView.game.grid.field;
        Tile[][] undoField = mView.game.grid.undoField;
        editor.putInt(WIDTH, field.length);
        editor.putInt(HEIGHT, field.length);

        for (int xx = 0; xx < field.length; xx++)
        {
            for (int yy = 0; yy < field[0].length; yy++)
            {
                if (field[xx][yy] != null)
                    editor.putInt(CURRENT_STATE + rows + " " + xx + " " + yy, field[xx][yy].getValue());
                else
                    editor.putInt(CURRENT_STATE + rows + " " + xx + " " + yy, 0);

                if (undoField[xx][yy] != null)
                    editor.putInt(UNDO_GRID + rows + " " + xx + " " + yy, undoField[xx][yy].getValue());
                else
                    editor.putInt(UNDO_GRID + rows + " " + xx + " " + yy, 0);
            }
        }

        // reward deletions:
        editor.putInt(REWARD_DELETES, MainActivity.mRewardDeletes);

        // game values:
        editor.putLong(SCORE, mView.game.score);
        editor.putLong(UNDO_SCORE, mView.game.lastScore);
        editor.putBoolean(CAN_UNDO, mView.game.canUndo);
        editor.putInt(GAME_STATE, mView.game.gameState);
        editor.putInt(UNDO_GAME_STATE, mView.game.lastGameState);
        editor.putBoolean("has_snapshot" + rows, true); // important
        editor.apply();

        mView.game.makeToast(R.string.saved);
    }
}