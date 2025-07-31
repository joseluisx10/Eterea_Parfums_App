package com.example.etereatesis;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.etereatesis.adaptadores.DetalleItemAdapter;
import com.example.etereatesis.models.DetalleItem;

import java.io.File;
import java.io.FileOutputStream;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FacturaDetalleActivity extends AppCompatActivity {

    private TextView tvSubtotal, tvDescuento, tvRecargo, tvTotal;
    private RecyclerView recyclerDetalleItems;
    private DetalleItemAdapter detalleItemAdapter;
    private final List<DetalleItem> listaDetalleItems = new ArrayList<>();
    private Button btnExportar;

    private static final String CONNECTION_URL = DETECTOR.CONNECTION_URL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_factura_detalle);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }


        tvSubtotal   = findViewById(R.id.tvSubtotal);
        tvDescuento  = findViewById(R.id.tvDescuento);
        tvRecargo    = findViewById(R.id.tvRecargo);
        tvTotal      = findViewById(R.id.tvTotal);
        recyclerDetalleItems = findViewById(R.id.recyclerDetalleItems);
        btnExportar  = findViewById(R.id.btnExportar);

        recyclerDetalleItems.setLayoutManager(new LinearLayoutManager(this));
        detalleItemAdapter = new DetalleItemAdapter(listaDetalleItems, this);
        recyclerDetalleItems.setAdapter(detalleItemAdapter);

        int numFactura = getIntent().getIntExtra("numFactura", -1);
        if (numFactura == -1) {
            Toast.makeText(this, "Número de factura inválido", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        cargarDetalleFactura(numFactura);
        btnExportar.setOnClickListener(v -> exportarAFacturaPDF());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /* ---------- CONSULTA SQL ---------- */
    private void cargarDetalleFactura(int numFactura) {
        new Thread(() -> {
            try (Connection cn = DriverManager.getConnection(CONNECTION_URL)) {

                Class.forName("net.sourceforge.jtds.jdbc.Driver");
                CallableStatement st = cn.prepareCall("{ call sp_ObtenerDetalleFactura(?) }");
                st.setInt(1, numFactura);
                boolean has = st.execute();

                List<DetalleItem> tmp = new ArrayList<>();

                if (has) {
                    ResultSet rs = st.getResultSet();
                    while (rs.next()) {
                        tmp.add(new DetalleItem(
                                rs.getString("Descripcion"),
                                rs.getInt("Cantidad"),
                                rs.getDouble("PrecioUnitario"),
                                rs.getDouble("SubtotalItem"),
                                rs.getInt("PromocionID")));
                    }
                    rs.close();
                }

                if (st.getMoreResults()) {
                    ResultSet rs2 = st.getResultSet();
                    if (rs2.next()) {
                        double subtotal  = rs2.getDouble("subtotal_general");
                        double desc      = rs2.getDouble("descuento");
                        double rec       = rs2.getDouble("recargo_tarjeta");
                        double total     = rs2.getDouble("precio_total");

                        runOnUiThread(() -> {
                            NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("es","AR"));
                            tvSubtotal .setText("Subtotal: "  + nf.format(subtotal));
                            tvDescuento.setText("Descuento: " + nf.format(desc));
                            tvRecargo  .setText("Recargo: "   + nf.format(rec));
                            tvTotal    .setText("Total: "     + nf.format(total));
                        });
                    }
                    rs2.close();
                }
                st.close();

                runOnUiThread(() -> {
                    listaDetalleItems.clear();
                    listaDetalleItems.addAll(tmp);
                    detalleItemAdapter.notifyDataSetChanged();
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(this,"Error: "+e.getMessage(),Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    /* ---------- PDF ---------- */
    private void exportarAFacturaPDF() {
        try {
            /* ───────── 1. Inflar la plantilla de la factura ───────── */
            LayoutInflater inflater = LayoutInflater.from(this);
            View pdfView = inflater.inflate(R.layout.invoice_layout, null);

            /* ───────── 2. Rellenar datos de cabecera ───────── */
            ((TextView) pdfView.findViewById(R.id.inv_tvSubtotal))
                    .setText(tvSubtotal.getText());
            ((TextView) pdfView.findViewById(R.id.inv_tvDescuento))
                    .setText(tvDescuento.getText());
            ((TextView) pdfView.findViewById(R.id.inv_tvRecargo))
                    .setText(tvRecargo.getText());
            ((TextView) pdfView.findViewById(R.id.inv_tvTotal))
                    .setText(tvTotal.getText());

            /* ───────── 3. Cargar lista de ítems en el RecyclerView interno ───────── */
            RecyclerView rv = pdfView.findViewById(R.id.inv_rvItems);
            rv.setLayoutManager(new LinearLayoutManager(this));
            rv.setAdapter(new DetalleItemAdapter(listaDetalleItems, this));

            /* ───────── 4. Medir y maquetar la vista (A4 ≈ 595×842 px @72 dpi) ───────── */
            int dpi   = getResources().getDisplayMetrics().densityDpi;
            int width = (int) (595f * dpi / 72f);
            pdfView.measure(
                    View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            );
            pdfView.layout(0, 0,
                    pdfView.getMeasuredWidth(),
                    pdfView.getMeasuredHeight());

            /* ───────── 5. Crear y rellenar PdfDocument ───────── */
            PdfDocument document = new PdfDocument();
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(
                    pdfView.getMeasuredWidth(),
                    pdfView.getMeasuredHeight(),
                    1
            ).create();
            PdfDocument.Page page = document.startPage(pageInfo);
            Canvas canvas = page.getCanvas();
            pdfView.draw(canvas);
            document.finishPage(page);

            /* ───────── 6. Guardar en almacenamiento interno de la app ───────── */
            File dir = getExternalFilesDir(null);
            if (dir == null) dir = getFilesDir();
            File pdfFile = new File(dir, "factura_" + System.currentTimeMillis() + ".pdf");
            try (FileOutputStream fos = new FileOutputStream(pdfFile)) {
                document.writeTo(fos);
            }
            document.close();

            /* ───────── 7. Abrir con un chooser de visor de PDF ───────── */
            Uri uri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    pdfFile
            );

            Intent intent = new Intent(Intent.ACTION_VIEW)
                    .setDataAndType(uri, "application/pdf")
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            Intent chooser = Intent.createChooser(intent, "Abrir PDF con");
            startActivity(chooser);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this,
                    "Error generando/abriendo PDF: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }


}
