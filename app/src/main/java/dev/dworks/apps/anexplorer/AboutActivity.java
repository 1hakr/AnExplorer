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

import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ShareCompat;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import dev.dworks.apps.anexplorer.misc.Utils;
import dev.dworks.apps.anexplorer.setting.SettingsActivity;

public class AboutActivity extends ActionBarActivity {


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_about);

        final Resources res = getResources();
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		TextView logo = (TextView)findViewById(R.id.logo);
        logo.setTextColor(SettingsActivity.getActionBarColor(this));
        String header = logo.getText() + getSuffix() + (Utils.isTelevision(this)? " for Android TV" : "") + " v" + BuildConfig.VERSION_NAME;
		logo.setText(header);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.about, menu);
        if(isNonPlay()){
            menu.removeItem(R.id.action_rate);
            menu.removeItem(R.id.action_support);
        } else if(Utils.isTelevision(this)){
			menu.removeItem(R.id.action_feedback);
			menu.removeItem(R.id.action_github);
			menu.removeItem(R.id.action_gplus);
			menu.removeItem(R.id.action_twitter);
		}
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
			ShareCompat.IntentBuilder
					.from(this)
					.setEmailTo(new String[]{"hakr@dworks.in"})
					.setSubject("AnExplorer Feedback" + getSuffix())
					.setType("text/email")
					.setChooserTitle("Send Feedback")
					.startChooser();
			break;
		case R.id.action_rate:
			Intent intentMarket = new Intent("android.intent.action.VIEW");
			intentMarket.setData(Uri.parse("market://details?id=" + BuildConfig.APPLICATION_ID));
			startActivity(intentMarket);
			break;
		case R.id.action_support:
			Intent intentMarketAll = new Intent("android.intent.action.VIEW");
			intentMarketAll.setData(Uri.parse("market://search?q=pub:DWorkS"));
			startActivity(intentMarketAll);
			break;
		}
		return super.onOptionsItemSelected(item);
	}

    @Override
    public void startActivity(Intent intent) {
        if(Utils.isIntentAvailable(this, intent)) {
            super.startActivity(intent);
        }
    }

    private String getSuffix(){
        return Utils.isProVersion() ? " Pro" : "";
    }

    private boolean isNonPlay(){
        return BuildConfig.FLAVOR.contains("other");
    }
}