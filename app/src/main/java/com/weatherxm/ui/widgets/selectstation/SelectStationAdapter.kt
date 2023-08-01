package com.weatherxm.ui.widgets.selectstation

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.weatherxm.R
import com.weatherxm.data.DeviceProfile
import com.weatherxm.data.services.CacheService
import com.weatherxm.databinding.ListItemWidgetSelectStationBinding
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.util.DateTimeHelper.getRelativeFormattedTime
import com.weatherxm.util.ResourcesHelper
import com.weatherxm.util.Weather
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SelectStationAdapter(private val stationListener: (UIDevice) -> Unit) :
    ListAdapter<UIDevice, SelectStationAdapter.SelectStationViewHolder>(
        SelectStationDiffCallback()
    ), KoinComponent {

    val resHelper: ResourcesHelper by inject()

    private var selectedPosition: Int = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SelectStationViewHolder {
        val binding =
            ListItemWidgetSelectStationBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        return SelectStationViewHolder(binding, stationListener)
    }

    override fun onBindViewHolder(holder: SelectStationViewHolder, position: Int) {
        holder.bind(getItem(position), selectedPosition == position)
    }

    inner class SelectStationViewHolder(
        private val binding: ListItemWidgetSelectStationBinding,
        private val stationListener: (UIDevice) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {

        private lateinit var device: UIDevice

        init {
            binding.root.setOnClickListener {
                stationListener.invoke(device)

                // Change the selected state
                val lastSelectedItem = selectedPosition
                selectedPosition = adapterPosition

                // Notify unselected & selected items, to force UI update
                notifyItemChanged(lastSelectedItem)
                notifyItemChanged(selectedPosition)
            }
        }

        @SuppressLint("SetTextI18n")
        fun bind(item: UIDevice, isSelected: Boolean) {
            binding.root.isSelected = isSelected
            binding.selectedIcon.visibility = if (isSelected) {
                View.VISIBLE
            } else {
                View.GONE
            }

            this.device = item
            binding.name.text = item.getDefaultOrFriendlyName()

            binding.address.text = if (item.address.isNullOrEmpty()) {
                resHelper.getString(R.string.unknown_address)
            } else {
                item.address
            }

            if (item.currentWeather == null || item.currentWeather.isEmpty()) {
                binding.weatherDataLayout.visibility = View.GONE
                binding.noDataLayout.visibility = View.VISIBLE
            } else {
                setWeatherData(item)
            }

            setStatus(item)
        }

        private fun setStatus(device: UIDevice) {
            with(binding.lastSeen) {
                text = device.lastWeatherStationActivity?.getRelativeFormattedTime(
                    fallbackIfTooSoon = context.getString(R.string.just_now)
                )
            }

            binding.statusIcon.setImageResource(
                if (device.profile == DeviceProfile.Helium) {
                    R.drawable.ic_helium
                } else {
                    R.drawable.ic_wifi
                }
            )

            binding.statusCard.setCardBackgroundColor(
                itemView.context.getColor(
                    when (device.isActive) {
                        true -> R.color.successTint
                        false -> R.color.errorTint
                        else -> R.color.midGrey
                    }
                )
            )
        }

        private fun setWeatherData(device: UIDevice) {
            binding.icon.setAnimation(Weather.getWeatherAnimation(device.currentWeather?.icon))
            binding.temperature.text = Weather.getFormattedTemperature(
                device.currentWeather?.temperature, 1, includeUnit = false
            )
            binding.temperatureUnit.text = Weather.getPreferredUnit(
                resHelper.getString(CacheService.KEY_TEMPERATURE),
                resHelper.getString(R.string.temperature_celsius)
            )
            binding.feelsLike.text = Weather.getFormattedTemperature(
                device.currentWeather?.feelsLike, 1, includeUnit = false
            )
            binding.feelsLikeUnit.text = Weather.getPreferredUnit(
                resHelper.getString(CacheService.KEY_TEMPERATURE),
                resHelper.getString(R.string.temperature_celsius)
            )
            binding.humidity.setData(
                Weather.getFormattedHumidity(
                    device.currentWeather?.humidity, includeUnit = false
                ), "%"
            )
            val windValue = Weather.getFormattedWind(
                device.currentWeather?.windSpeed,
                device.currentWeather?.windDirection,
                includeUnits = false
            )
            val windUnit = Weather.getPreferredUnit(
                resHelper.getString(CacheService.KEY_WIND),
                resHelper.getString(R.string.wind_speed_ms)
            )
            val windDirectionUnit = device.currentWeather?.windDirection?.let {
                Weather.getFormattedWindDirection(it)
            } ?: ""
            binding.wind.setData(windValue, "$windUnit $windDirectionUnit")

            binding.rain.setData(
                Weather.getFormattedPrecipitation(
                    device.currentWeather?.precipitation, includeUnit = false
                ), Weather.getPrecipitationPreferredUnit()
            )
        }
    }

    class SelectStationDiffCallback : DiffUtil.ItemCallback<UIDevice>() {

        override fun areItemsTheSame(oldItem: UIDevice, newItem: UIDevice): Boolean {
            return oldItem.id == newItem.id
        }

        @Suppress("MaxLineLength", "CyclomaticComplexMethod")
        override fun areContentsTheSame(oldItem: UIDevice, newItem: UIDevice): Boolean {
            return oldItem.id == newItem.id &&
                oldItem.name == newItem.name &&
                oldItem.address == newItem.address
        }
    }
}
