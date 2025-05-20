package com.unk.recoverrework;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.chad.library.adapter4.BaseQuickAdapter;
import com.chad.library.adapter4.viewholder.QuickViewHolder;
import com.minew.ble.mst03.bean.MST03Entity;
import com.minew.ble.mst03.frames.CombinationFrame;
import com.minew.ble.mst03.frames.DeviceStaticInfoFrame;
import com.minew.ble.v3.enums.FrameType;

import java.util.List;

// MST03Adapter extiende de BaseQuickAdapter para manejar y mostrar datos de MST03Entity en un RecyclerView.
public class MST03Adapter extends BaseQuickAdapter<MST03Entity, QuickViewHolder> {

    // Variable para almacenar el recurso de diseño que se utilizará para cada elemento en el RecyclerView.
    private final int layoutRes;

    // Constructor del adaptador que recibe el recurso de diseño y la lista de datos a mostrar.
    public MST03Adapter(int layoutRes, List<MST03Entity> data) {
        super();
        this.layoutRes = layoutRes;
    }

    // Método que se llama para vincular los datos de un objeto MST03Entity a un ViewHolder.
    @Override
    protected void onBindViewHolder(@NonNull QuickViewHolder holder, int i, @Nullable MST03Entity entity) {
        // Variables para almacenar los diferentes frames de datos que se pueden extraer del dispositivo.
        DeviceStaticInfoFrame deviceFrame = null;
        CombinationFrame combinationFrame = null;

        // Si el objeto de dispositivo no es nulo, intenta obtener los frames específicos de información.
        if (entity != null) {
            // Intenta obtener el frame de información estática del dispositivo.
            if (entity.getMinewFrame(FrameType.DEVICE_INFORMATION_FRAME) != null) {
                deviceFrame = (DeviceStaticInfoFrame) entity.getMinewFrame(FrameType.DEVICE_INFORMATION_FRAME);
            }

            // Intenta obtener el frame de tipo CombinationFrame.
            if (entity.getMinewFrame(FrameType.COMBINATION_FRAME) != null) {
                combinationFrame = (CombinationFrame) entity.getMinewFrame(FrameType.COMBINATION_FRAME);
            }
        }

        // Si se obtuvo el frame de información del dispositivo, actualiza los campos del ViewHolder con estos datos.
        if (deviceFrame != null) {
            holder.setText(R.id.tv_device_name, "Nombre: Recover 03")
                    .setText(R.id.tv_device_mac, "ID: " + entity.getMacAddress()) // Muestra la dirección MAC del dispositivo.
                    .setText(R.id.tv_device_battery, "Batería: " + deviceFrame.getBattery() + "%"); // Muestra el nivel de batería.
        }

        // Si se obtuvo el frame de combinación, actualiza los datos de temperatura y los hace visibles.
        if (combinationFrame != null) {
            holder.setText(R.id.tv_device_ht, "T°: " + combinationFrame.getTemperature() + "°C");
            holder.setVisible(R.id.tv_device_ht, true); // Muestra la temperatura.
        } else {
            holder.setVisible(R.id.tv_device_ht, false); // Si no hay datos de temperatura, oculta este campo.
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