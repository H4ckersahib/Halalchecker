package com.halalcheck.app.data.local;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import com.halalcheck.app.data.local.dao.IngredientDao;
import com.halalcheck.app.data.local.dao.IngredientDao_Impl;
import com.halalcheck.app.data.local.dao.ProductDao;
import com.halalcheck.app.data.local.dao.ProductDao_Impl;
import com.halalcheck.app.data.local.dao.ScanHistoryDao;
import com.halalcheck.app.data.local.dao.ScanHistoryDao_Impl;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class HalalDatabase_Impl extends HalalDatabase {
  private volatile IngredientDao _ingredientDao;

  private volatile ProductDao _productDao;

  private volatile ScanHistoryDao _scanHistoryDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(1) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `ingredients` (`id` TEXT NOT NULL, `name_de` TEXT NOT NULL, `name_en` TEXT NOT NULL, `name_tr` TEXT, `name_ar` TEXT, `default_category` TEXT NOT NULL, `hanafi_ruling` TEXT, `maliki_ruling` TEXT, `shafi_ruling` TEXT, `hanbali_ruling` TEXT, `reason` TEXT, `reason_de` TEXT, `source` TEXT, `alternatives` TEXT, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `products` (`barcode` TEXT NOT NULL, `name` TEXT NOT NULL, `brand` TEXT, `image_url` TEXT, `ingredients_text` TEXT NOT NULL, `halal_status` TEXT NOT NULL, `confidence` REAL NOT NULL, `source` TEXT NOT NULL, `last_updated` INTEGER NOT NULL, PRIMARY KEY(`barcode`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `scan_history` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `barcode` TEXT NOT NULL, `product_name` TEXT NOT NULL, `brand` TEXT, `image_url` TEXT, `halal_status` TEXT NOT NULL, `scanned_at` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'eba28a2a8a7947566d89a9d777e15105')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `ingredients`");
        db.execSQL("DROP TABLE IF EXISTS `products`");
        db.execSQL("DROP TABLE IF EXISTS `scan_history`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsIngredients = new HashMap<String, TableInfo.Column>(14);
        _columnsIngredients.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsIngredients.put("name_de", new TableInfo.Column("name_de", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsIngredients.put("name_en", new TableInfo.Column("name_en", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsIngredients.put("name_tr", new TableInfo.Column("name_tr", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsIngredients.put("name_ar", new TableInfo.Column("name_ar", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsIngredients.put("default_category", new TableInfo.Column("default_category", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsIngredients.put("hanafi_ruling", new TableInfo.Column("hanafi_ruling", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsIngredients.put("maliki_ruling", new TableInfo.Column("maliki_ruling", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsIngredients.put("shafi_ruling", new TableInfo.Column("shafi_ruling", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsIngredients.put("hanbali_ruling", new TableInfo.Column("hanbali_ruling", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsIngredients.put("reason", new TableInfo.Column("reason", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsIngredients.put("reason_de", new TableInfo.Column("reason_de", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsIngredients.put("source", new TableInfo.Column("source", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsIngredients.put("alternatives", new TableInfo.Column("alternatives", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysIngredients = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesIngredients = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoIngredients = new TableInfo("ingredients", _columnsIngredients, _foreignKeysIngredients, _indicesIngredients);
        final TableInfo _existingIngredients = TableInfo.read(db, "ingredients");
        if (!_infoIngredients.equals(_existingIngredients)) {
          return new RoomOpenHelper.ValidationResult(false, "ingredients(com.halalcheck.app.data.local.entity.IngredientEntity).\n"
                  + " Expected:\n" + _infoIngredients + "\n"
                  + " Found:\n" + _existingIngredients);
        }
        final HashMap<String, TableInfo.Column> _columnsProducts = new HashMap<String, TableInfo.Column>(9);
        _columnsProducts.put("barcode", new TableInfo.Column("barcode", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProducts.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProducts.put("brand", new TableInfo.Column("brand", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProducts.put("image_url", new TableInfo.Column("image_url", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProducts.put("ingredients_text", new TableInfo.Column("ingredients_text", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProducts.put("halal_status", new TableInfo.Column("halal_status", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProducts.put("confidence", new TableInfo.Column("confidence", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProducts.put("source", new TableInfo.Column("source", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProducts.put("last_updated", new TableInfo.Column("last_updated", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysProducts = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesProducts = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoProducts = new TableInfo("products", _columnsProducts, _foreignKeysProducts, _indicesProducts);
        final TableInfo _existingProducts = TableInfo.read(db, "products");
        if (!_infoProducts.equals(_existingProducts)) {
          return new RoomOpenHelper.ValidationResult(false, "products(com.halalcheck.app.data.local.entity.ProductEntity).\n"
                  + " Expected:\n" + _infoProducts + "\n"
                  + " Found:\n" + _existingProducts);
        }
        final HashMap<String, TableInfo.Column> _columnsScanHistory = new HashMap<String, TableInfo.Column>(7);
        _columnsScanHistory.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsScanHistory.put("barcode", new TableInfo.Column("barcode", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsScanHistory.put("product_name", new TableInfo.Column("product_name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsScanHistory.put("brand", new TableInfo.Column("brand", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsScanHistory.put("image_url", new TableInfo.Column("image_url", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsScanHistory.put("halal_status", new TableInfo.Column("halal_status", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsScanHistory.put("scanned_at", new TableInfo.Column("scanned_at", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysScanHistory = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesScanHistory = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoScanHistory = new TableInfo("scan_history", _columnsScanHistory, _foreignKeysScanHistory, _indicesScanHistory);
        final TableInfo _existingScanHistory = TableInfo.read(db, "scan_history");
        if (!_infoScanHistory.equals(_existingScanHistory)) {
          return new RoomOpenHelper.ValidationResult(false, "scan_history(com.halalcheck.app.data.local.entity.ScanHistoryEntity).\n"
                  + " Expected:\n" + _infoScanHistory + "\n"
                  + " Found:\n" + _existingScanHistory);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "eba28a2a8a7947566d89a9d777e15105", "20c36ee24d827bbc515597faf859b2e5");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "ingredients","products","scan_history");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `ingredients`");
      _db.execSQL("DELETE FROM `products`");
      _db.execSQL("DELETE FROM `scan_history`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(IngredientDao.class, IngredientDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(ProductDao.class, ProductDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(ScanHistoryDao.class, ScanHistoryDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public IngredientDao ingredientDao() {
    if (_ingredientDao != null) {
      return _ingredientDao;
    } else {
      synchronized(this) {
        if(_ingredientDao == null) {
          _ingredientDao = new IngredientDao_Impl(this);
        }
        return _ingredientDao;
      }
    }
  }

  @Override
  public ProductDao productDao() {
    if (_productDao != null) {
      return _productDao;
    } else {
      synchronized(this) {
        if(_productDao == null) {
          _productDao = new ProductDao_Impl(this);
        }
        return _productDao;
      }
    }
  }

  @Override
  public ScanHistoryDao scanHistoryDao() {
    if (_scanHistoryDao != null) {
      return _scanHistoryDao;
    } else {
      synchronized(this) {
        if(_scanHistoryDao == null) {
          _scanHistoryDao = new ScanHistoryDao_Impl(this);
        }
        return _scanHistoryDao;
      }
    }
  }
}
