package com.unk.recoverrework;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.chad.library.adapter4.BaseQuickAdapter;
import com.chad.library.adapter4.viewholder.QuickViewHolder;
import com.ylwl.industry.bean.IndustrialHtSensor;
import com.ylwl.industry.enums.HtFrameType;
import com.ylwl.industry.frames.DeviceStaticInfoFrame;
import com.ylwl.industry.frames.IndustrialHtFrame;

import java.util.List;

// MST01Adapter extiende de BaseQuickAdapter, y está diseñado para manejar y mostrar datos de IndustrialHtSensor en un RecyclerView.
public class MST01Adapter extends BaseQuickAdapter<IndustrialHtSensor, QuickViewHolder> {

    // Variable para almacenar el recurso de diseño que se utilizará para cada elemento en el RecyclerView.
    private final int layoutRes;

    // Constructor del adaptador que recibe el recurso de diseño y la lista de datos a mostrar.
    public MST01Adapter(int layoutRes, List<IndustrialHtSensor> data) {
        super();
        this.layoutRes = layoutRes;
    }

    // Método que se llama para vincular los datos de un objeto IndustrialHtSensor a un ViewHolder.
    @Override
    protected void onBindViewHolder(@NonNull QuickViewHolder holder, int i, @Nullable IndustrialHtSensor entity) {
        // Variables para almacenar los diferentes frames de datos que se pueden extraer del sensor.
        IndustrialHtFrame industrialFrame = null;
        DeviceStaticInfoFrame deviceFrame = null;

        // Si el objeto de sensor no es nulo, intenta obtener los frames específicos de información.
        if (entity != null) {
            // Intenta obtener el frame de tipo IndustrialHtFrame del sensor.
            if (entity.getMinewFrame(HtFrameType.INDUSTRIAL_HT_FRAME) != null) {
                industrialFrame = (IndustrialHtFrame) entity.getMinewFrame(HtFrameType.INDUSTRIAL_HT_FRAME);
            }

            // Intenta obtener el frame de información estática del dispositivo.
            if (entity.getMinewFrame(HtFrameType.DEVICE_STATIC_INFO_FRAME) != null) {
                deviceFrame = (DeviceStaticInfoFrame) entity.getMinewFrame(HtFrameType.DEVICE_STATIC_INFO_FRAME);
            }
        }

        // Si se obtuvo el frame de información del dispositivo, actualiza los campos del ViewHolder con estos datos.
        if (deviceFrame != null) {
            holder.setText(R.id.tv_device_name, "Nombre: Recover 01")
                    .setText(R.id.tv_device_mac, "ID: " + entity.getMacAddress()) // Muestra la dirección MAC del dispositivo.
                    .setText(R.id.tv_device_battery, "Batería: " + deviceFrame.getBattery() + "%"); // Muestra el nivel de batería.
        }

        // Si se obtuvo el frame de temperatura y humedad, actualiza estos datos en el ViewHolder y los hace visibles.
        if (industrialFrame != null) {
            holder.setText(R.id.tv_device_ht, "T°: " + industrialFrame.getTemperature() + "°C | Humedad: " + industrialFrame.getHumidity() + "%")
                    .setVisible(R.id.tv_device_ht, true); // Muestra la temperatura y la humedad.
        } else {
            holder.setVisible(R.id.tv_device_ht, false); // Si no hay datos de temperatura y humedad, oculta este campo.
        }
    }

    // Método para crear un nuevo ViewHolder inflando el diseño especificado.
    @NonNull
    @Override
    protected QuickViewHolder onCreateViewHolder(@NonNull Context ctx, @NonNull ViewGroup viewGroup, int viewType) {
        // Infla el diseño de un elemento de la lista basado en el recurso de diseño especificado.
        View v = LayoutInflater.from(ctx).inflate(layoutRes, viewGroup, false);
        return new QuickViewHolder(v); // Crea y devuelve un nuevo ViewHolder con el diseño inflado.
    }
}
