/*
 * Copyright (C) 2014 Hari Krishna Dulipudi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.dworks.apps.anexplorer;

import dev.dworks.apps.anexplorer.misc.ViewCompat;
import dev.dworks.apps.anexplorer.setting.SettingsActivity;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnTouchListener;
import android.widget.TextView;

public class AboutActivity extends Activity {


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_about);
		
        final Resources res = getResources();
        boolean mShowAsDialog = res.getBoolean(R.bool.show_as_dialog);

        if (mShowAsDialog) {
        	if(SettingsActivity.getAsDialog(this)){
                // backgroundDimAmount from theme isn't applied; do it manually
                final WindowManager.LayoutParams a = getWindow().getAttributes();
                a.dimAmount = 0.6f;
                getWindow().setAttributes(a);

                getWindow().setFlags(0, WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
                getWindow().setFlags(~0, WindowManager.LayoutParams.FLAG_DIM_BEHIND);

                // Inset ourselves to look like a dialog
                final Point size = new Point();
                getWindowManager().getDefaultDisplay().getSize(size);

                final int width = (int) res.getFraction(R.dimen.dialog_about_width, size.x, size.x);
                final int height = (int) res.getFraction(R.dimen.dialog_about_height, size.y, size.y);
                final int insetX = (size.x - width) / 2;
                final int insetY = (size.y - height) / 2;

                final Drawable before = getWindow().getDecorView().getBackground();
                final Drawable after = new InsetDrawable(before, insetX, insetY, insetX, insetY);
                ViewCompat.setBackground(getWindow().getDecorView(), after);

                // Dismiss when touch down in the dimmed inset area
                getWindow().getDecorView().setOnTouchListener(new OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        if (event.getAction() == MotionEvent.ACTION_DOWN) {
                            final float x = event.getX();
                            final float y = event.getY();
                            if (x < insetX || x > v.getWidth() - insetX || y < insetY
                                    || y > v.getHeight() - insetY) {
                                finish();
                                return true;
                            }
                        }
                        return false;
                    }
                });	
        	}
        }
		getActionBar().setDisplayHomeAsUpEnabled(true);
		TextView logo = (TextView)findViewById(R.id.logo);
		logo.setText(logo.getText() + " v" + DocumentsApplication.APP_VERSION);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.about, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			break;
		case R.id.action_github:
			startActivity(new Intent("android.intent.action.VIEW",
					Uri.parse("https://github.com/DWorkS")));
			break;
		case R.id.action_gplus:
			startActivity(new Intent("android.intent.action.VIEW",
					Uri.parse("https://plus.google.com/+HariKrishnaDulipudi")));
			break;
		case R.id.action_twitter:
			startActivity(new Intent("android.intent.action.VIEW",
					Uri.parse("https://twitter.com/1HaKr")));
			break;
		case R.id.action_feedback:
			Intent intentFeedback = new Intent("android.intent.action.SEND");
			intentFeedback.setType("text/email");
			intentFeedback.putExtra("android.intent.extra.EMAIL", new String[]{"hakr@dworks.in"});
			intentFeedback.putExtra("android.intent.extra.SUBJECT", "AnExplorer Feedback");
			startActivity(Intent.createChooser(intentFeedback, "Send Feedback"));
			break;
		case R.id.action_rate:
			Intent intentMarket = new Intent("android.intent.action.VIEW");
			intentMarket.setData(Uri.parse("market://details?id=dev.dworks.apps.anexplorer"));
			startActivity(intentMarket);
			break;
		case R.id.action_support:
			Intent intentMarketAll = new Intent("android.intent.action.VIEW");
			intentMarketAll.setData(Uri.parse("market://search?q=pub:D WorkS"));
			startActivity(intentMarketAll);
			break;
		}
		return super.onOptionsItemSelected(item);
	}
}