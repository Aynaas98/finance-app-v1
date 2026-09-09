package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.StockHoldingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StockHoldingDao {
    @Query("SELECT * FROM stock_holdings ORDER BY ticker ASC")
    fun getAllStockHoldings(): Flow<List<StockHoldingEntity>>

    @Query("SELECT * FROM stock_holdings WHERE id = :id")
    suspend fun getStockHoldingById(id: Long): StockHoldingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStockHolding(holding: StockHoldingEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(holdings: List<StockHoldingEntity>)

    @Update
    suspend fun updateStockHolding(holding: StockHoldingEntity)

    @Delete
    suspend fun deleteStockHolding(holding: StockHoldingEntity)

    @Query("SELECT COUNT(*) FROM stock_holdings")
    suspend fun countStockHoldings(): Int
}
