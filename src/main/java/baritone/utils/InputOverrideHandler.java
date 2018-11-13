/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package baritone.utils;

import baritone.Baritone;
import baritone.api.event.events.TickEvent;
import baritone.api.utils.IInputOverrideHandler;
import baritone.api.utils.input.Input;
import baritone.behavior.Behavior;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;

import java.util.HashMap;
import java.util.Map;

/**
 * An interface with the game's control system allowing the ability to
 * force down certain controls, having the same effect as if we were actually
 * physically forcing down the assigned key.
 *
 * @author Brady
 * @since 7/31/2018 11:20 PM
 */
public final class InputOverrideHandler extends Behavior implements IInputOverrideHandler {

    public InputOverrideHandler(Baritone baritone) {
        super(baritone);
    }

    /**
     * Maps inputs to whether or not we are forcing their state down.
     */
    private final Map<Input, Boolean> inputForceStateMap = new HashMap<>();

    /**
     * Returns whether or not we are forcing down the specified {@link KeyBinding}.
     *
     * @param key The KeyBinding object
     * @return Whether or not it is being forced down
     */
    @Override
    public final boolean isInputForcedDown(KeyBinding key) {
        return isInputForcedDown(Input.getInputForBind(key));
    }

    /**
     * Returns whether or not we are forcing down the specified {@link Input}.
     *
     * @param input The input
     * @return Whether or not it is being forced down
     */
    @Override
    public final boolean isInputForcedDown(Input input) {
        return input == null ? false : this.inputForceStateMap.getOrDefault(input, false);
    }

    /**
     * Sets whether or not the specified {@link Input} is being forced down.
     *
     * @param input  The {@link Input}
     * @param forced Whether or not the state is being forced
     */
    @Override
    public final void setInputForceState(Input input, boolean forced) {
        this.inputForceStateMap.put(input, forced);
    }

    /**
     * Clears the override state for all keys
     */
    @Override
    public final void clearAllKeys() {
        this.inputForceStateMap.clear();
    }

    @Override
    public final void onProcessKeyBinds() {
        // Simulate the key being held down this tick
        for (Input input : Input.values()) {
            KeyBinding keyBinding = input.getKeyBinding();

            if (isInputForcedDown(keyBinding) && !keyBinding.isKeyDown()) {
                int keyCode = keyBinding.getKeyCode();

                if (keyCode < Keyboard.KEYBOARD_SIZE) {
                    KeyBinding.onTick(keyCode < 0 ? keyCode + 100 : keyCode);
                }
            }
        }
    }

    @Override
    public final void onTick(TickEvent event) {
        if (event.getType() == TickEvent.Type.OUT) {
            return;
        }
        if (Baritone.settings().leftClickWorkaround.get()) {
            boolean stillClick = BlockBreakHelper.tick(isInputForcedDown(Input.CLICK_LEFT.getKeyBinding()));
            setInputForceState(Input.CLICK_LEFT, stillClick);
        }
    }
}
