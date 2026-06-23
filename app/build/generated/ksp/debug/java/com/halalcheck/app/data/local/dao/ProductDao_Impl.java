package com.halalcheck.app.data.local.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.halalcheck.app.data.local.entity.ProductEntity;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class ProductDao_Impl implements ProductDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<ProductEntity> __insertionAdapterOfProductEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteProduct;

  public ProductDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfProductEntity = new EntityInsertionAdapter<ProductEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `products` (`barcode`,`name`,`brand`,`image_url`,`ingredients_text`,`halal_status`,`confidence`,`source`,`last_updated`) VALUES (?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ProductEntity entity) {
        statement.bindString(1, entity.getBarcode());
        statement.bindString(2, entity.getName());
        if (entity.getBrand() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getBrand());
        }
        if (entity.getImageUrl() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getImageUrl());
        }
        statement.bindString(5, entity.getIngredientsText());
        statement.bindString(6, entity.getHalalStatus());
        statement.bindDouble(7, entity.getConfidence());
        statement.bindString(8, entity.getSource());
        statement.bindLong(9, entity.getLastUpdated());
      }
    };
    this.__preparedStmtOfDeleteProduct = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM products WHERE barcode = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertProduct(final ProductEntity product,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfProductEntity.insert(product);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteProduct(final String barcode, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteProduct.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, barcode);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteProduct.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object getProductByBarcode(final String barcode,
      final Continuation<? super ProductEntity> $completion) {
    final String _sql = "SELECT * FROM products WHERE barcode = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, barcode);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<ProductEntity>() {
      @Override
      @Nullable
      public ProductEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfBarcode = CursorUtil.getColumnIndexOrThrow(_cursor, "barcode");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfBrand = CursorUtil.getColumnIndexOrThrow(_cursor, "brand");
          final int _cursorIndexOfImageUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "image_url");
          final int _cursorIndexOfIngredientsText = CursorUtil.getColumnIndexOrThrow(_cursor, "ingredients_text");
          final int _cursorIndexOfHalalStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "halal_status");
          final int _cursorIndexOfConfidence = CursorUtil.getColumnIndexOrThrow(_cursor, "confidence");
          final int _cursorIndexOfSource = CursorUtil.getColumnIndexOrThrow(_cursor, "source");
          final int _cursorIndexOfLastUpdated = CursorUtil.getColumnIndexOrThrow(_cursor, "last_updated");
          final ProductEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpBarcode;
            _tmpBarcode = _cursor.getString(_cursorIndexOfBarcode);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpBrand;
            if (_cursor.isNull(_cursorIndexOfBrand)) {
              _tmpBrand = null;
            } else {
              _tmpBrand = _cursor.getString(_cursorIndexOfBrand);
            }
            final String _tmpImageUrl;
            if (_cursor.isNull(_cursorIndexOfImageUrl)) {
              _tmpImageUrl = null;
            } else {
              _tmpImageUrl = _cursor.getString(_cursorIndexOfImageUrl);
            }
            final String _tmpIngredientsText;
            _tmpIngredientsText = _cursor.getString(_cursorIndexOfIngredientsText);
            final String _tmpHalalStatus;
            _tmpHalalStatus = _cursor.getString(_cursorIndexOfHalalStatus);
            final float _tmpConfidence;
            _tmpConfidence = _cursor.getFloat(_cursorIndexOfConfidence);
            final String _tmpSource;
            _tmpSource = _cursor.getString(_cursorIndexOfSource);
            final long _tmpLastUpdated;
            _tmpLastUpdated = _cursor.getLong(_cursorIndexOfLastUpdated);
            _result = new ProductEntity(_tmpBarcode,_tmpName,_tmpBrand,_tmpImageUrl,_tmpIngredientsText,_tmpHalalStatus,_tmpConfidence,_tmpSource,_tmpLastUpdated);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<ProductEntity>> getAllProductsFlow() {
    final String _sql = "SELECT * FROM products ORDER BY last_updated DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"products"}, new Callable<List<ProductEntity>>() {
      @Override
      @NonNull
      public List<ProductEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfBarcode = CursorUtil.getColumnIndexOrThrow(_cursor, "barcode");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfBrand = CursorUtil.getColumnIndexOrThrow(_cursor, "brand");
          final int _cursorIndexOfImageUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "image_url");
          final int _cursorIndexOfIngredientsText = CursorUtil.getColumnIndexOrThrow(_cursor, "ingredients_text");
          final int _cursorIndexOfHalalStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "halal_status");
          final int _cursorIndexOfConfidence = CursorUtil.getColumnIndexOrThrow(_cursor, "confidence");
          final int _cursorIndexOfSource = CursorUtil.getColumnIndexOrThrow(_cursor, "source");
          final int _cursorIndexOfLastUpdated = CursorUtil.getColumnIndexOrThrow(_cursor, "last_updated");
          final List<ProductEntity> _result = new ArrayList<ProductEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ProductEntity _item;
            final String _tmpBarcode;
            _tmpBarcode = _cursor.getString(_cursorIndexOfBarcode);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpBrand;
            if (_cursor.isNull(_cursorIndexOfBrand)) {
              _tmpBrand = null;
            } else {
              _tmpBrand = _cursor.getString(_cursorIndexOfBrand);
            }
            final String _tmpImageUrl;
            if (_cursor.isNull(_cursorIndexOfImageUrl)) {
              _tmpImageUrl = null;
            } else {
              _tmpImageUrl = _cursor.getString(_cursorIndexOfImageUrl);
            }
            final String _tmpIngredientsText;
            _tmpIngredientsText = _cursor.getString(_cursorIndexOfIngredientsText);
            final String _tmpHalalStatus;
            _tmpHalalStatus = _cursor.getString(_cursorIndexOfHalalStatus);
            final float _tmpConfidence;
            _tmpConfidence = _cursor.getFloat(_cursorIndexOfConfidence);
            final String _tmpSource;
            _tmpSource = _cursor.getString(_cursorIndexOfSource);
            final long _tmpLastUpdated;
            _tmpLastUpdated = _cursor.getLong(_cursorIndexOfLastUpdated);
            _item = new ProductEntity(_tmpBarcode,_tmpName,_tmpBrand,_tmpImageUrl,_tmpIngredientsText,_tmpHalalStatus,_tmpConfidence,_tmpSource,_tmpLastUpdated);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object searchProducts(final String query,
      final Continuation<? super List<ProductEntity>> $completion) {
    final String _sql = "SELECT * FROM products WHERE name LIKE '%' || ? || '%' OR brand LIKE '%' || ? || '%'";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, query);
    _argIndex = 2;
    _statement.bindString(_argIndex, query);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<ProductEntity>>() {
      @Override
      @NonNull
      public List<ProductEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfBarcode = CursorUtil.getColumnIndexOrThrow(_cursor, "barcode");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfBrand = CursorUtil.getColumnIndexOrThrow(_cursor, "brand");
          final int _cursorIndexOfImageUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "image_url");
          final int _cursorIndexOfIngredientsText = CursorUtil.getColumnIndexOrThrow(_cursor, "ingredients_text");
          final int _cursorIndexOfHalalStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "halal_status");
          final int _cursorIndexOfConfidence = CursorUtil.getColumnIndexOrThrow(_cursor, "confidence");
          final int _cursorIndexOfSource = CursorUtil.getColumnIndexOrThrow(_cursor, "source");
          final int _cursorIndexOfLastUpdated = CursorUtil.getColumnIndexOrThrow(_cursor, "last_updated");
          final List<ProductEntity> _result = new ArrayList<ProductEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ProductEntity _item;
            final String _tmpBarcode;
            _tmpBarcode = _cursor.getString(_cursorIndexOfBarcode);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpBrand;
            if (_cursor.isNull(_cursorIndexOfBrand)) {
              _tmpBrand = null;
            } else {
              _tmpBrand = _cursor.getString(_cursorIndexOfBrand);
            }
            final String _tmpImageUrl;
            if (_cursor.isNull(_cursorIndexOfImageUrl)) {
              _tmpImageUrl = null;
            } else {
              _tmpImageUrl = _cursor.getString(_cursorIndexOfImageUrl);
            }
            final String _tmpIngredientsText;
            _tmpIngredientsText = _cursor.getString(_cursorIndexOfIngredientsText);
            final String _tmpHalalStatus;
            _tmpHalalStatus = _cursor.getString(_cursorIndexOfHalalStatus);
            final float _tmpConfidence;
            _tmpConfidence = _cursor.getFloat(_cursorIndexOfConfidence);
            final String _tmpSource;
            _tmpSource = _cursor.getString(_cursorIndexOfSource);
            final long _tmpLastUpdated;
            _tmpLastUpdated = _cursor.getLong(_cursorIndexOfLastUpdated);
            _item = new ProductEntity(_tmpBarcode,_tmpName,_tmpBrand,_tmpImageUrl,_tmpIngredientsText,_tmpHalalStatus,_tmpConfidence,_tmpSource,_tmpLastUpdated);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getProductCount(final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(*) FROM products";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
