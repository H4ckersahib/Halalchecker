package com.halalcheck.app.data.local.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.halalcheck.app.data.local.entity.ScanHistoryEntity;
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
public final class ScanHistoryDao_Impl implements ScanHistoryDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<ScanHistoryEntity> __insertionAdapterOfScanHistoryEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteScan;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAllScans;

  public ScanHistoryDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfScanHistoryEntity = new EntityInsertionAdapter<ScanHistoryEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `scan_history` (`id`,`barcode`,`product_name`,`brand`,`image_url`,`halal_status`,`scanned_at`) VALUES (nullif(?, 0),?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ScanHistoryEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getBarcode());
        statement.bindString(3, entity.getProductName());
        if (entity.getBrand() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getBrand());
        }
        if (entity.getImageUrl() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getImageUrl());
        }
        statement.bindString(6, entity.getHalalStatus());
        statement.bindLong(7, entity.getScannedAt());
      }
    };
    this.__preparedStmtOfDeleteScan = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM scan_history WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteAllScans = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM scan_history";
        return _query;
      }
    };
  }

  @Override
  public Object insertScan(final ScanHistoryEntity scan,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfScanHistoryEntity.insert(scan);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteScan(final long id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteScan.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, id);
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
          __preparedStmtOfDeleteScan.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteAllScans(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAllScans.acquire();
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
          __preparedStmtOfDeleteAllScans.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<ScanHistoryEntity>> getAllScansFlow() {
    final String _sql = "SELECT * FROM scan_history ORDER BY scanned_at DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"scan_history"}, new Callable<List<ScanHistoryEntity>>() {
      @Override
      @NonNull
      public List<ScanHistoryEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfBarcode = CursorUtil.getColumnIndexOrThrow(_cursor, "barcode");
          final int _cursorIndexOfProductName = CursorUtil.getColumnIndexOrThrow(_cursor, "product_name");
          final int _cursorIndexOfBrand = CursorUtil.getColumnIndexOrThrow(_cursor, "brand");
          final int _cursorIndexOfImageUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "image_url");
          final int _cursorIndexOfHalalStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "halal_status");
          final int _cursorIndexOfScannedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "scanned_at");
          final List<ScanHistoryEntity> _result = new ArrayList<ScanHistoryEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ScanHistoryEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpBarcode;
            _tmpBarcode = _cursor.getString(_cursorIndexOfBarcode);
            final String _tmpProductName;
            _tmpProductName = _cursor.getString(_cursorIndexOfProductName);
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
            final String _tmpHalalStatus;
            _tmpHalalStatus = _cursor.getString(_cursorIndexOfHalalStatus);
            final long _tmpScannedAt;
            _tmpScannedAt = _cursor.getLong(_cursorIndexOfScannedAt);
            _item = new ScanHistoryEntity(_tmpId,_tmpBarcode,_tmpProductName,_tmpBrand,_tmpImageUrl,_tmpHalalStatus,_tmpScannedAt);
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
  public Flow<List<ScanHistoryEntity>> getRecentScansFlow(final int limit) {
    final String _sql = "SELECT * FROM scan_history ORDER BY scanned_at DESC LIMIT ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, limit);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"scan_history"}, new Callable<List<ScanHistoryEntity>>() {
      @Override
      @NonNull
      public List<ScanHistoryEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfBarcode = CursorUtil.getColumnIndexOrThrow(_cursor, "barcode");
          final int _cursorIndexOfProductName = CursorUtil.getColumnIndexOrThrow(_cursor, "product_name");
          final int _cursorIndexOfBrand = CursorUtil.getColumnIndexOrThrow(_cursor, "brand");
          final int _cursorIndexOfImageUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "image_url");
          final int _cursorIndexOfHalalStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "halal_status");
          final int _cursorIndexOfScannedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "scanned_at");
          final List<ScanHistoryEntity> _result = new ArrayList<ScanHistoryEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ScanHistoryEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpBarcode;
            _tmpBarcode = _cursor.getString(_cursorIndexOfBarcode);
            final String _tmpProductName;
            _tmpProductName = _cursor.getString(_cursorIndexOfProductName);
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
            final String _tmpHalalStatus;
            _tmpHalalStatus = _cursor.getString(_cursorIndexOfHalalStatus);
            final long _tmpScannedAt;
            _tmpScannedAt = _cursor.getLong(_cursorIndexOfScannedAt);
            _item = new ScanHistoryEntity(_tmpId,_tmpBarcode,_tmpProductName,_tmpBrand,_tmpImageUrl,_tmpHalalStatus,_tmpScannedAt);
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
  public Flow<List<ScanHistoryEntity>> getScansByStatusFlow(final String status) {
    final String _sql = "SELECT * FROM scan_history WHERE halal_status = ? ORDER BY scanned_at DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, status);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"scan_history"}, new Callable<List<ScanHistoryEntity>>() {
      @Override
      @NonNull
      public List<ScanHistoryEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfBarcode = CursorUtil.getColumnIndexOrThrow(_cursor, "barcode");
          final int _cursorIndexOfProductName = CursorUtil.getColumnIndexOrThrow(_cursor, "product_name");
          final int _cursorIndexOfBrand = CursorUtil.getColumnIndexOrThrow(_cursor, "brand");
          final int _cursorIndexOfImageUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "image_url");
          final int _cursorIndexOfHalalStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "halal_status");
          final int _cursorIndexOfScannedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "scanned_at");
          final List<ScanHistoryEntity> _result = new ArrayList<ScanHistoryEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ScanHistoryEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpBarcode;
            _tmpBarcode = _cursor.getString(_cursorIndexOfBarcode);
            final String _tmpProductName;
            _tmpProductName = _cursor.getString(_cursorIndexOfProductName);
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
            final String _tmpHalalStatus;
            _tmpHalalStatus = _cursor.getString(_cursorIndexOfHalalStatus);
            final long _tmpScannedAt;
            _tmpScannedAt = _cursor.getLong(_cursorIndexOfScannedAt);
            _item = new ScanHistoryEntity(_tmpId,_tmpBarcode,_tmpProductName,_tmpBrand,_tmpImageUrl,_tmpHalalStatus,_tmpScannedAt);
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
  public Flow<List<ScanHistoryEntity>> searchScansFlow(final String query) {
    final String _sql = "SELECT * FROM scan_history WHERE product_name LIKE '%' || ? || '%' ORDER BY scanned_at DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, query);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"scan_history"}, new Callable<List<ScanHistoryEntity>>() {
      @Override
      @NonNull
      public List<ScanHistoryEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfBarcode = CursorUtil.getColumnIndexOrThrow(_cursor, "barcode");
          final int _cursorIndexOfProductName = CursorUtil.getColumnIndexOrThrow(_cursor, "product_name");
          final int _cursorIndexOfBrand = CursorUtil.getColumnIndexOrThrow(_cursor, "brand");
          final int _cursorIndexOfImageUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "image_url");
          final int _cursorIndexOfHalalStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "halal_status");
          final int _cursorIndexOfScannedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "scanned_at");
          final List<ScanHistoryEntity> _result = new ArrayList<ScanHistoryEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ScanHistoryEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpBarcode;
            _tmpBarcode = _cursor.getString(_cursorIndexOfBarcode);
            final String _tmpProductName;
            _tmpProductName = _cursor.getString(_cursorIndexOfProductName);
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
            final String _tmpHalalStatus;
            _tmpHalalStatus = _cursor.getString(_cursorIndexOfHalalStatus);
            final long _tmpScannedAt;
            _tmpScannedAt = _cursor.getLong(_cursorIndexOfScannedAt);
            _item = new ScanHistoryEntity(_tmpId,_tmpBarcode,_tmpProductName,_tmpBrand,_tmpImageUrl,_tmpHalalStatus,_tmpScannedAt);
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
  public Object getScanCount(final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(*) FROM scan_history";
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

  @Override
  public Object getScanCountSince(final long since,
      final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(*) FROM scan_history WHERE scanned_at >= ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, since);
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
