/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.itemmanager.views;

import com.badlogic.gdx.math.Vector2;

import forge.Forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;

/**
 * A quick converter to avoid -1 being displayed for unapplicable values.
 */
public class IntegerRenderer extends ItemCellRenderer {
    @Override
    public void draw(Graphics g, Object value, FSkinFont font, FSkinColor foreColor, Vector2 loc, float itemWidth, float itemHeight) {
        if ((int)value == -1) { value = "-"; }
        super.draw(g, value, font, foreColor, loc, itemWidth, itemHeight);
    }
}