package cat.uab.tradumatica.tripnote;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * TripNote: app de práctica para Localización de aplicaciones (Màster Tradumàtica, UAB).
 * Contiene errores de internacionalización intencionados que se deben encontrar al testear.
 */
public class MainActivity extends Activity {

    /** Datos de ejemplo. Los nombres propios son contenido, no interfaz: no se traducen. */
    private static class Trip {
        final String city;
        final int typeRes;
        final Calendar start;
        final double budget;
        final int daysLeft;
        final String addedBy;
        final String hotel;
        final String museum;
        final String foodPlace;

        Trip(String city, int typeRes, int year, int month, int day, double budget, int daysLeft,
             String addedBy, String hotel, String museum, String foodPlace) {
            this.city = city;
            this.typeRes = typeRes;
            this.start = Calendar.getInstance();
            this.start.set(year, month, day);
            this.budget = budget;
            this.daysLeft = daysLeft;
            this.addedBy = addedBy;
            this.hotel = hotel;
            this.museum = museum;
            this.foodPlace = foodPlace;
        }
    }

    private static final String USER_NAME = "Alex";
    private static final String VERSION = "1.0";

    private final List<Trip> trips = new ArrayList<>();
    private Trip current;

    private View screenHome;
    private View screenTrip;
    private View screenSettings;
    private TextView tabHome;
    private TextView tabTrip;
    private TextView tabSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        trips.add(new Trip("Lisboa", R.string.trip_type_city, 2027, Calendar.MARCH, 7, 1250.5, 1,
                "Marta", "Hotel Alfama", "Museu Calouste Gulbenkian", "Time Out Market"));
        trips.add(new Trip("Menorca", R.string.trip_type_beach, 2027, Calendar.JULY, 3, 980.0, 12,
                "Jordi", "Hotel Binibeca", "Museu de Menorca", "Cala Galdana Beach Bar"));
        trips.add(new Trip("Vall d'Aran", R.string.trip_type_mountain, 2027, Calendar.OCTOBER, 11,
                640.75, 21, "Laia", "Refugi de Colomèrs", "Museu dera Val d'Aran", "Casa Irene"));

        screenHome = findViewById(R.id.screen_home);
        screenTrip = findViewById(R.id.screen_trip);
        screenSettings = findViewById(R.id.screen_settings);
        tabHome = findViewById(R.id.tab_home);
        tabTrip = findViewById(R.id.tab_trip);
        tabSettings = findViewById(R.id.tab_settings);

        tabHome.setOnClickListener(v -> showTab(0));
        tabTrip.setOnClickListener(v -> showTab(1));
        tabSettings.setOnClickListener(v -> showTab(2));

        View.OnClickListener soon = v ->
                Toast.makeText(this, R.string.coming_soon, Toast.LENGTH_SHORT).show();
        findViewById(R.id.btn_plan).setOnClickListener(soon);
        findViewById(R.id.btn_import).setOnClickListener(soon);
        findViewById(R.id.btn_open_map).setOnClickListener(soon);
        findViewById(R.id.btn_book).setOnClickListener(v ->
                Toast.makeText(this, R.string.trip_booked, Toast.LENGTH_SHORT).show());
        findViewById(R.id.btn_delete).setOnClickListener(v -> confirmDelete());

        setUpSettings();
        renderHome();
        renderTrip();
        showTab(0);
    }

    // ------------------------------------------------------------------ Inicio

    private void renderHome() {
        TextView greeting = findViewById(R.id.home_greeting);
        greeting.setText(getString(R.string.home_greeting, USER_NAME));

        TextView count = findViewById(R.id.home_count);
        count.setText(getString(R.string.home_count_prefix) + " " + trips.size() + " "
                + getString(R.string.home_count_suffix));

        TextView tip = findViewById(R.id.home_tip);
        tip.setText(getText(R.string.home_tip));

        LinearLayout list = findViewById(R.id.trip_list);
        list.removeAllViews();
        if (trips.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText(R.string.home_empty);
            empty.setPadding(0, dp(12), 0, 0);
            list.addView(empty);
        }
        for (Trip trip : trips) {
            list.addView(buildCard(trip));
        }
    }

    private View buildCard(Trip trip) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(Color.WHITE);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(10);
        card.setLayoutParams(lp);

        TextView title = new TextView(this);
        title.setText(trip.city);
        title.setTextSize(18);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(Color.parseColor("#1F2A44"));
        card.addView(title);

        TextView type = new TextView(this);
        type.setText(getString(trip.typeRes) + " " + getString(R.string.trip_word));
        type.setTextColor(Color.parseColor("#5A6275"));
        card.addView(type);

        TextView days = new TextView(this);
        days.setText(getResources().getQuantityString(R.plurals.days_left, trip.daysLeft, trip.daysLeft));
        days.setTextColor(Color.parseColor("#B26A00"));
        card.addView(days);

        card.setOnClickListener(v -> {
            current = trip;
            renderTrip();
            showTab(1);
        });
        return card;
    }

    // ------------------------------------------------------------------ Viaje

    private void renderTrip() {
        View empty = findViewById(R.id.trip_empty);
        View content = findViewById(R.id.trip_content);
        if (current == null) {
            empty.setVisibility(View.VISIBLE);
            content.setVisibility(View.GONE);
            return;
        }
        empty.setVisibility(View.GONE);
        content.setVisibility(View.VISIBLE);

        ((TextView) findViewById(R.id.trip_title)).setText(current.city);

        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
        ((TextView) findViewById(R.id.trip_dates)).setText(
                getString(R.string.trip_dates_label) + " " + dateFormat.format(current.start.getTime()));

        ((TextView) findViewById(R.id.trip_budget)).setText(
                getString(R.string.trip_budget_label) + " $" + String.format(Locale.US, "%,.2f", current.budget));

        ((TextView) findViewById(R.id.trip_added)).setText(
                getString(R.string.trip_added_by, current.addedBy, current.city));
        ((TextView) findViewById(R.id.trip_hotel)).setText(current.hotel);
        ((TextView) findViewById(R.id.trip_museum)).setText(current.museum);
        ((TextView) findViewById(R.id.trip_food_place)).setText(current.foodPlace);
    }

    private void confirmDelete() {
        if (current == null) {
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_delete_title)
                .setMessage(getString(R.string.dialog_delete_message, current.city))
                .setPositiveButton(R.string.dialog_delete_confirm, (dialog, which) -> {
                    trips.remove(current);
                    current = null;
                    renderHome();
                    renderTrip();
                    showTab(0);
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    // ------------------------------------------------------------------ Ajustes

    private void setUpSettings() {
        Spinner units = findViewById(R.id.spinner_units);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.units, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        units.setAdapter(adapter);

        Button save = findViewById(R.id.btn_save);
        save.setOnClickListener(v ->
                Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show());

        ((TextView) findViewById(R.id.settings_version)).setText(
                getString(R.string.settings_version, VERSION));
    }

    // ------------------------------------------------------------------ Pestañas

    private void showTab(int index) {
        screenHome.setVisibility(index == 0 ? View.VISIBLE : View.GONE);
        screenTrip.setVisibility(index == 1 ? View.VISIBLE : View.GONE);
        screenSettings.setVisibility(index == 2 ? View.VISIBLE : View.GONE);
        styleTab(tabHome, index == 0);
        styleTab(tabTrip, index == 1);
        styleTab(tabSettings, index == 2);
    }

    private void styleTab(TextView tab, boolean selected) {
        tab.setTextColor(Color.parseColor(selected ? "#C77700" : "#5A6275"));
        tab.setTypeface(selected ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
