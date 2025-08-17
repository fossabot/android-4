package uk.trigpointing.android;

import uk.trigpointing.android.common.BaseActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.webkit.WebView;

public class OldAboutActivity extends BaseActivity {

	public static final String PAGE="PAGE";

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.old_about_page);
		
		// Enable back button in action bar
		if (getSupportActionBar() != null) {
			getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		}
		
		Bundle extras = getIntent().getExtras();
		if (extras == null) {return;}
		String url = String.format("file:///android_asset/%s", extras.getString(PAGE));

        WebView mWebView = findViewById(R.id.helppage);
		mWebView.loadUrl(url);

		
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			// Handle back button in action bar
			finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

}
