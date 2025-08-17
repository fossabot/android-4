package uk.trigpointing.android;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import com.mikepenz.aboutlibraries.LibsBuilder;

import uk.trigpointing.android.common.BaseActivity;

public class AboutActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        TextView appNameVersion = findViewById(R.id.app_name_version);
        try {
            String versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            appNameVersion.setText(getString(R.string.app_name_version, versionName));
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        TextView privacyPolicy = findViewById(R.id.privacy_policy);
        privacyPolicy.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://trigpointing.uk/wiki/TrigpointingUK:Privacy_policy"));
            startActivity(intent);
        });

        Button openSourceLicenses = findViewById(R.id.open_source_licenses);
        openSourceLicenses.setOnClickListener(v -> new LibsBuilder()
                .withActivityTitle("Open Source Licenses")
                .withAboutVersionShown(true)
                .start(this));

        Button acknowledgements = findViewById(R.id.acknowledgements);
        acknowledgements.setOnClickListener(v -> {
            Intent intent = new Intent(this, AcknowledgementsActivity.class);
            startActivity(intent);
        });
    }
}
