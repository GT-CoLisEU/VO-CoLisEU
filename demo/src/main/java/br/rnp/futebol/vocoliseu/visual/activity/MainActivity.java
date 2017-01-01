package br.rnp.futebol.vocoliseu.visual.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.exoplayer2.demo.PlayerActivity;
import com.google.android.exoplayer2.demo.R;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import br.rnp.futebol.vocoliseu.dao.TExpForListDAO;
import br.rnp.futebol.vocoliseu.dao.TExperimentDAO;
import br.rnp.futebol.vocoliseu.dao.TScriptDAO;
import br.rnp.futebol.vocoliseu.pojo.TExperiment;
import br.rnp.futebol.vocoliseu.pojo.TScript;
import br.rnp.futebol.vocoliseu.util.adapter.ExperimentAdapter;
import br.rnp.futebol.vocoliseu.util.adapter.SelectableExperimentAdapter;
import br.rnp.futebol.vocoliseu.visual.activity.experiment.ExperimentGeneralActivity;
import br.rnp.futebol.vocoliseu.visual.activity.script.ScriptGeneralActivity;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private ListView lvExperiments, lvAux;
    private ArrayList<TExperiment> exps;
    private TExpForListDAO dao;
    private final int SELECT_FILE_CODE = 7;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        checkPerm();

        lvExperiments = (ListView) findViewById(R.id.lv_main_experiments);
        lvAux = new ListView(this);

//        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab_experiment);
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//
//            }
//        });

        dao = new TExpForListDAO(getBaseContext());
        exps = dao.getExpsByNames(dao.getExpsNames());

        lvExperiments.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                TExperiment exp = exps.get(position);
                makeDialog(exp);

            }
        });

        lvExperiments.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                Intent mailClient = new Intent(Intent.ACTION_VIEW);
                mailClient.setClassName("com.google.android.gm", "com.google.android.gm.ConversationListActivity");
                startActivity(mailClient);
                return false;
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.open, R.string.close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case SELECT_FILE_CODE:
                if (resultCode == RESULT_OK) {
                    Uri uri = data.getData();
                    importExp(getPath(this, uri));
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public String getPath(Context context, Uri uri) {
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            String[] projection = {"_data"};
            Cursor cursor = null;

            try {
                cursor = context.getContentResolver().query(uri, projection, null, null, null);
                if (cursor != null) {
                    int column_index = cursor.getColumnIndexOrThrow("_data");
                    if (cursor.moveToFirst())
                        return cursor.getString(column_index);
                }
            } catch (Exception e) {
                // Eat it
            } finally {
                if (cursor != null)
                    cursor.close();
            }
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    private void importExp(String path) {
        String json = read(path);
        boolean success = false;
        try {
            TExperiment experiment = new TExperiment().fromJson(new JSONObject(json));
            if (experiment != null) {
                TExpForListDAO dao = new TExpForListDAO(getBaseContext());
                dao.insert(experiment);
                dao.close();
                refreshList();
                success = true;
            }
        } catch (Exception e) {
            // eat it
        }
        Toast.makeText(getBaseContext(), success ? "Success!" : "Could not lad the file.", Toast.LENGTH_SHORT).show();
    }

    private String read(String file) {
        try {
//            String csv = Environment.getExternalStorageDirectory().getAbsolutePath().concat(file);
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String text = "", line;
            while ((line = reader.readLine()) != null) {
                text += line.concat(" ");
            }
            return text;
        } catch (IOException e) {
            return "";
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshList();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    private void refreshList() {
        exps = dao.getExpsByNames(dao.getExpsNames());
        if (exps != null) {
            ExperimentAdapter adapter = new ExperimentAdapter(getBaseContext(), exps);
            lvExperiments.setAdapter(adapter);
        }
    }

    private void checkPerm() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            }
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.dashboard_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
//        int id = item.getItemId();

        //noinspection SimplifiableIfStatement

        return super.onOptionsItemSelected(item);
    }

    public AlertDialog makeDialog(final TExperiment exp) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(exp.getInstruction());
        builder.setTitle("Start Experiment ".concat(exp.getName()));
        builder.setPositiveButton("Start", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                int index = 0;
                TScript first = exp.getScripts().get(index);

                String provider = first.getProvider();

                Bundle extras = new Bundle();
                extras.putInt("index", index);
                extras.putSerializable("experiment", exp);

                Intent intent = new Intent((getBaseContext()), PlayerActivity.class);
                intent.putExtras(extras);

                intent.setData(Uri.parse(provider));
                intent.setAction(PlayerActivity.ACTION_VIEW);

                startActivity(intent);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
        return dialog;
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        switch (id) {
            case (R.id.navigation_item_experiment):
                if ((new TScriptDAO(this)).getScriptsCount() > 0)
                    startActivity(new Intent(this, ExperimentGeneralActivity.class));
                else
                    Toast.makeText(getBaseContext(), "No script has been created.\nBefore creating an experiments, configure some videos!", Toast.LENGTH_SHORT).show();
                break;
            case (R.id.navigation_item_video):
                startActivity(new Intent(this, ScriptGeneralActivity.class));
                break;
            case (R.id.navigation_item_import):
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("text/plain");
                startActivityForResult(intent, SELECT_FILE_CODE);
                break;
            case (R.id.navigation_item_export):
                if ((new TExperimentDAO(this)).getExpsCount() > 0)
                    exportExps();
                else
                    Toast.makeText(getBaseContext(), "No experiment available to export", Toast.LENGTH_SHORT).show();
                break;
            default:
                break;

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }


    private void refreshExportList(ArrayList<TExperiment> exps, ArrayList<TExperiment> exps2) {
        for (TExperiment te : exps2)
            te.setUsedAux(false);
        for (TExperiment e : exps)
            for (TExperiment te : exps2)
                if (te.getFilename().equals(e.getFilename()))
                    te.setUsedAux(true);

        SelectableExperimentAdapter selAdapter = new SelectableExperimentAdapter(getBaseContext(), exps2);
        lvAux.setAdapter(selAdapter);
    }

    private void exportExps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final ArrayList<TExperiment> experiments = new ArrayList<>();
        final ArrayList<TExperiment> auxExps = exps;

        refreshExportList(experiments, auxExps);
        lvAux.setOnItemClickListener(new ListView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> listView, View itemView, int position, long itemId) {
                boolean added = false;
                int count = 0;
                for (TExperiment e : experiments)
                    if (e.getFilename().equals(exps.get(position).getFilename()))
                        added = true;
                    else
                        count++;
                if (!added)
                    experiments.add(exps.get(position));
                else
                    experiments.remove(count);
                refreshExportList(experiments, auxExps);
            }
        });

        builder.setNegativeButton(
                "cancel",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        dialog.dismiss();
                    }
                });

        builder.setPositiveButton("send", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent emailIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
                ArrayList<Uri> uris = new ArrayList<>();
                for (TExperiment e : experiments) {
                    File filelocation = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), e.getFilename().concat(".txt"));
//                    filelocation.setReadable(true, false);
                    Uri path = Uri.fromFile(filelocation);
                    uris.add(path);
                }
                emailIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
//                emailIntent.putExtra(Intent.EXTRA_STREAM, uris.get(0));
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, "VO-CoLisEU experiment(s)");
                emailIntent.setType("file/*");
                startActivity(emailIntent);
                dialog.cancel();
                dialog.dismiss();
            }
        });

        builder.setTitle("Select one or more experiments:");
        builder.setView(lvAux);
        builder.show();
    }
}
