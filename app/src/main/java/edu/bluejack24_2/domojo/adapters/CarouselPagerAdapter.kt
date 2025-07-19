package edu.bluejack24_2.domojo.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import edu.bluejack24_2.domojo.databinding.ItemCarouselBinding
import edu.bluejack24_2.domojo.models.CarouselItem

class CarouselPagerAdapter : RecyclerView.Adapter<CarouselPagerAdapter.CarouselPageViewHolder>() {

    private var items: List<CarouselItem> = emptyList() // Internal list of carousel items

    fun updateItems(newItems: List<CarouselItem>) {
        this.items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CarouselPageViewHolder {
        val binding = ItemCarouselBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CarouselPageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CarouselPageViewHolder, position: Int) {
        val carouselItem = items[position]
        Log.d("Carousel Adapter", "onBindViewHolder: Binding position $position, " +
                "Heading: '${carouselItem.heading}', Description: '${carouselItem.description}', " +
                "ImageUrl: '${carouselItem.imageUrl}'")

        holder.bind(carouselItem)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    class CarouselPageViewHolder(private val binding: ItemCarouselBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(carouselItem: CarouselItem) {
            binding.carouselItem = carouselItem // Binds the data to the layout
            binding.executePendingBindings() // Updates views immediately
        }
    }
}