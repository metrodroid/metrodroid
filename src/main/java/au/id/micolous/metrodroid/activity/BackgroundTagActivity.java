/*
 * BackgroundCardActivity.java
 *
 * Copyright 2014 Eric Butler
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
package au.id.micolous.metrodroid.activity;

import android.content.Intent;
import android.os.Bundle;

/**
 * @author Eric Butler
 */
public class BackgroundTagActivity extends MetrodroidActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = new Intent(this, ReadingTagActivity.class);
        intent.setAction(getIntent().getAction());
        intent.putExtras(getIntent().getExtras());
        startActivity(intent);

        finish();
    }
}
