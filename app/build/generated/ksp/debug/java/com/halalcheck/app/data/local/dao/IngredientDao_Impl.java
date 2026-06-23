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
import com.halalcheck.app.data.local.entity.IngredientEntity;
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
public final class IngredientDao_Impl implements IngredientDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<IngredientEntity> __insertionAdapterOfIngredientEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAll;

  public IngredientDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfIngredientEntity = new EntityInsertionAdapter<IngredientEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `ingredients` (`id`,`name_de`,`name_en`,`name_tr`,`name_ar`,`default_category`,`hanafi_ruling`,`maliki_ruling`,`shafi_ruling`,`hanbali_ruling`,`reason`,`reason_de`,`source`,`alternatives`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final IngredientEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getNameDe());
        statement.bindString(3, entity.getNameEn());
        if (entity.getNameTr() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getNameTr());
        }
        if (entity.getNameAr() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getNameAr());
        }
        statement.bindString(6, entity.getDefaultCategory());
        if (entity.getHanafiRuling() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getHanafiRuling());
        }
        if (entity.getMalikiRuling() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getMalikiRuling());
        }
        if (entity.getShafiRuling() == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.getShafiRuling());
        }
        if (entity.getHanbaliRuling() == null) {
          statement.bindNull(10);
        } else {
          statement.bindString(10, entity.getHanbaliRuling());
        }
        if (entity.getReason() == null) {
          statement.bindNull(11);
        } else {
          statement.bindString(11, entity.getReason());
        }
        if (entity.getReasonDe() == null) {
          statement.bindNull(12);
        } else {
          statement.bindString(12, entity.getReasonDe());
        }
        if (entity.getSource() == null) {
          statement.bindNull(13);
        } else {
          statement.bindString(13, entity.getSource());
        }
        if (entity.getAlternatives() == null) {
          statement.bindNull(14);
        } else {
          statement.bindString(14, entity.getAlternatives());
        }
      }
    };
    this.__preparedStmtOfDeleteAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM ingredients";
        return _query;
      }
    };
  }

  @Override
  public Object insertIngredient(final IngredientEntity ingredient,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfIngredientEntity.insert(ingredient);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertIngredients(final List<IngredientEntity> ingredients,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfIngredientEntity.insert(ingredients);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteAll(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAll.acquire();
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
          __preparedStmtOfDeleteAll.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object getAllIngredients(final Continuation<? super List<IngredientEntity>> $completion) {
    final String _sql = "SELECT * FROM ingredients";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<IngredientEntity>>() {
      @Override
      @NonNull
      public List<IngredientEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfNameDe = CursorUtil.getColumnIndexOrThrow(_cursor, "name_de");
          final int _cursorIndexOfNameEn = CursorUtil.getColumnIndexOrThrow(_cursor, "name_en");
          final int _cursorIndexOfNameTr = CursorUtil.getColumnIndexOrThrow(_cursor, "name_tr");
          final int _cursorIndexOfNameAr = CursorUtil.getColumnIndexOrThrow(_cursor, "name_ar");
          final int _cursorIndexOfDefaultCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "default_category");
          final int _cursorIndexOfHanafiRuling = CursorUtil.getColumnIndexOrThrow(_cursor, "hanafi_ruling");
          final int _cursorIndexOfMalikiRuling = CursorUtil.getColumnIndexOrThrow(_cursor, "maliki_ruling");
          final int _cursorIndexOfShafiRuling = CursorUtil.getColumnIndexOrThrow(_cursor, "shafi_ruling");
          final int _cursorIndexOfHanbaliRuling = CursorUtil.getColumnIndexOrThrow(_cursor, "hanbali_ruling");
          final int _cursorIndexOfReason = CursorUtil.getColumnIndexOrThrow(_cursor, "reason");
          final int _cursorIndexOfReasonDe = CursorUtil.getColumnIndexOrThrow(_cursor, "reason_de");
          final int _cursorIndexOfSource = CursorUtil.getColumnIndexOrThrow(_cursor, "source");
          final int _cursorIndexOfAlternatives = CursorUtil.getColumnIndexOrThrow(_cursor, "alternatives");
          final List<IngredientEntity> _result = new ArrayList<IngredientEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final IngredientEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpNameDe;
            _tmpNameDe = _cursor.getString(_cursorIndexOfNameDe);
            final String _tmpNameEn;
            _tmpNameEn = _cursor.getString(_cursorIndexOfNameEn);
            final String _tmpNameTr;
            if (_cursor.isNull(_cursorIndexOfNameTr)) {
              _tmpNameTr = null;
            } else {
              _tmpNameTr = _cursor.getString(_cursorIndexOfNameTr);
            }
            final String _tmpNameAr;
            if (_cursor.isNull(_cursorIndexOfNameAr)) {
              _tmpNameAr = null;
            } else {
              _tmpNameAr = _cursor.getString(_cursorIndexOfNameAr);
            }
            final String _tmpDefaultCategory;
            _tmpDefaultCategory = _cursor.getString(_cursorIndexOfDefaultCategory);
            final String _tmpHanafiRuling;
            if (_cursor.isNull(_cursorIndexOfHanafiRuling)) {
              _tmpHanafiRuling = null;
            } else {
              _tmpHanafiRuling = _cursor.getString(_cursorIndexOfHanafiRuling);
            }
            final String _tmpMalikiRuling;
            if (_cursor.isNull(_cursorIndexOfMalikiRuling)) {
              _tmpMalikiRuling = null;
            } else {
              _tmpMalikiRuling = _cursor.getString(_cursorIndexOfMalikiRuling);
            }
            final String _tmpShafiRuling;
            if (_cursor.isNull(_cursorIndexOfShafiRuling)) {
              _tmpShafiRuling = null;
            } else {
              _tmpShafiRuling = _cursor.getString(_cursorIndexOfShafiRuling);
            }
            final String _tmpHanbaliRuling;
            if (_cursor.isNull(_cursorIndexOfHanbaliRuling)) {
              _tmpHanbaliRuling = null;
            } else {
              _tmpHanbaliRuling = _cursor.getString(_cursorIndexOfHanbaliRuling);
            }
            final String _tmpReason;
            if (_cursor.isNull(_cursorIndexOfReason)) {
              _tmpReason = null;
            } else {
              _tmpReason = _cursor.getString(_cursorIndexOfReason);
            }
            final String _tmpReasonDe;
            if (_cursor.isNull(_cursorIndexOfReasonDe)) {
              _tmpReasonDe = null;
            } else {
              _tmpReasonDe = _cursor.getString(_cursorIndexOfReasonDe);
            }
            final String _tmpSource;
            if (_cursor.isNull(_cursorIndexOfSource)) {
              _tmpSource = null;
            } else {
              _tmpSource = _cursor.getString(_cursorIndexOfSource);
            }
            final String _tmpAlternatives;
            if (_cursor.isNull(_cursorIndexOfAlternatives)) {
              _tmpAlternatives = null;
            } else {
              _tmpAlternatives = _cursor.getString(_cursorIndexOfAlternatives);
            }
            _item = new IngredientEntity(_tmpId,_tmpNameDe,_tmpNameEn,_tmpNameTr,_tmpNameAr,_tmpDefaultCategory,_tmpHanafiRuling,_tmpMalikiRuling,_tmpShafiRuling,_tmpHanbaliRuling,_tmpReason,_tmpReasonDe,_tmpSource,_tmpAlternatives);
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
  public Flow<List<IngredientEntity>> getAllIngredientsFlow() {
    final String _sql = "SELECT * FROM ingredients";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"ingredients"}, new Callable<List<IngredientEntity>>() {
      @Override
      @NonNull
      public List<IngredientEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfNameDe = CursorUtil.getColumnIndexOrThrow(_cursor, "name_de");
          final int _cursorIndexOfNameEn = CursorUtil.getColumnIndexOrThrow(_cursor, "name_en");
          final int _cursorIndexOfNameTr = CursorUtil.getColumnIndexOrThrow(_cursor, "name_tr");
          final int _cursorIndexOfNameAr = CursorUtil.getColumnIndexOrThrow(_cursor, "name_ar");
          final int _cursorIndexOfDefaultCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "default_category");
          final int _cursorIndexOfHanafiRuling = CursorUtil.getColumnIndexOrThrow(_cursor, "hanafi_ruling");
          final int _cursorIndexOfMalikiRuling = CursorUtil.getColumnIndexOrThrow(_cursor, "maliki_ruling");
          final int _cursorIndexOfShafiRuling = CursorUtil.getColumnIndexOrThrow(_cursor, "shafi_ruling");
          final int _cursorIndexOfHanbaliRuling = CursorUtil.getColumnIndexOrThrow(_cursor, "hanbali_ruling");
          final int _cursorIndexOfReason = CursorUtil.getColumnIndexOrThrow(_cursor, "reason");
          final int _cursorIndexOfReasonDe = CursorUtil.getColumnIndexOrThrow(_cursor, "reason_de");
          final int _cursorIndexOfSource = CursorUtil.getColumnIndexOrThrow(_cursor, "source");
          final int _cursorIndexOfAlternatives = CursorUtil.getColumnIndexOrThrow(_cursor, "alternatives");
          final List<IngredientEntity> _result = new ArrayList<IngredientEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final IngredientEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpNameDe;
            _tmpNameDe = _cursor.getString(_cursorIndexOfNameDe);
            final String _tmpNameEn;
            _tmpNameEn = _cursor.getString(_cursorIndexOfNameEn);
            final String _tmpNameTr;
            if (_cursor.isNull(_cursorIndexOfNameTr)) {
              _tmpNameTr = null;
            } else {
              _tmpNameTr = _cursor.getString(_cursorIndexOfNameTr);
            }
            final String _tmpNameAr;
            if (_cursor.isNull(_cursorIndexOfNameAr)) {
              _tmpNameAr = null;
            } else {
              _tmpNameAr = _cursor.getString(_cursorIndexOfNameAr);
            }
            final String _tmpDefaultCategory;
            _tmpDefaultCategory = _cursor.getString(_cursorIndexOfDefaultCategory);
            final String _tmpHanafiRuling;
            if (_cursor.isNull(_cursorIndexOfHanafiRuling)) {
              _tmpHanafiRuling = null;
            } else {
              _tmpHanafiRuling = _cursor.getString(_cursorIndexOfHanafiRuling);
            }
            final String _tmpMalikiRuling;
            if (_cursor.isNull(_cursorIndexOfMalikiRuling)) {
              _tmpMalikiRuling = null;
            } else {
              _tmpMalikiRuling = _cursor.getString(_cursorIndexOfMalikiRuling);
            }
            final String _tmpShafiRuling;
            if (_cursor.isNull(_cursorIndexOfShafiRuling)) {
              _tmpShafiRuling = null;
            } else {
              _tmpShafiRuling = _cursor.getString(_cursorIndexOfShafiRuling);
            }
            final String _tmpHanbaliRuling;
            if (_cursor.isNull(_cursorIndexOfHanbaliRuling)) {
              _tmpHanbaliRuling = null;
            } else {
              _tmpHanbaliRuling = _cursor.getString(_cursorIndexOfHanbaliRuling);
            }
            final String _tmpReason;
            if (_cursor.isNull(_cursorIndexOfReason)) {
              _tmpReason = null;
            } else {
              _tmpReason = _cursor.getString(_cursorIndexOfReason);
            }
            final String _tmpReasonDe;
            if (_cursor.isNull(_cursorIndexOfReasonDe)) {
              _tmpReasonDe = null;
            } else {
              _tmpReasonDe = _cursor.getString(_cursorIndexOfReasonDe);
            }
            final String _tmpSource;
            if (_cursor.isNull(_cursorIndexOfSource)) {
              _tmpSource = null;
            } else {
              _tmpSource = _cursor.getString(_cursorIndexOfSource);
            }
            final String _tmpAlternatives;
            if (_cursor.isNull(_cursorIndexOfAlternatives)) {
              _tmpAlternatives = null;
            } else {
              _tmpAlternatives = _cursor.getString(_cursorIndexOfAlternatives);
            }
            _item = new IngredientEntity(_tmpId,_tmpNameDe,_tmpNameEn,_tmpNameTr,_tmpNameAr,_tmpDefaultCategory,_tmpHanafiRuling,_tmpMalikiRuling,_tmpShafiRuling,_tmpHanbaliRuling,_tmpReason,_tmpReasonDe,_tmpSource,_tmpAlternatives);
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
  public Object getIngredientById(final String id,
      final Continuation<? super IngredientEntity> $completion) {
    final String _sql = "SELECT * FROM ingredients WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<IngredientEntity>() {
      @Override
      @Nullable
      public IngredientEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfNameDe = CursorUtil.getColumnIndexOrThrow(_cursor, "name_de");
          final int _cursorIndexOfNameEn = CursorUtil.getColumnIndexOrThrow(_cursor, "name_en");
          final int _cursorIndexOfNameTr = CursorUtil.getColumnIndexOrThrow(_cursor, "name_tr");
          final int _cursorIndexOfNameAr = CursorUtil.getColumnIndexOrThrow(_cursor, "name_ar");
          final int _cursorIndexOfDefaultCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "default_category");
          final int _cursorIndexOfHanafiRuling = CursorUtil.getColumnIndexOrThrow(_cursor, "hanafi_ruling");
          final int _cursorIndexOfMalikiRuling = CursorUtil.getColumnIndexOrThrow(_cursor, "maliki_ruling");
          final int _cursorIndexOfShafiRuling = CursorUtil.getColumnIndexOrThrow(_cursor, "shafi_ruling");
          final int _cursorIndexOfHanbaliRuling = CursorUtil.getColumnIndexOrThrow(_cursor, "hanbali_ruling");
          final int _cursorIndexOfReason = CursorUtil.getColumnIndexOrThrow(_cursor, "reason");
          final int _cursorIndexOfReasonDe = CursorUtil.getColumnIndexOrThrow(_cursor, "reason_de");
          final int _cursorIndexOfSource = CursorUtil.getColumnIndexOrThrow(_cursor, "source");
          final int _cursorIndexOfAlternatives = CursorUtil.getColumnIndexOrThrow(_cursor, "alternatives");
          final IngredientEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpNameDe;
            _tmpNameDe = _cursor.getString(_cursorIndexOfNameDe);
            final String _tmpNameEn;
            _tmpNameEn = _cursor.getString(_cursorIndexOfNameEn);
            final String _tmpNameTr;
            if (_cursor.isNull(_cursorIndexOfNameTr)) {
              _tmpNameTr = null;
            } else {
              _tmpNameTr = _cursor.getString(_cursorIndexOfNameTr);
            }
            final String _tmpNameAr;
            if (_cursor.isNull(_cursorIndexOfNameAr)) {
              _tmpNameAr = null;
            } else {
              _tmpNameAr = _cursor.getString(_cursorIndexOfNameAr);
            }
            final String _tmpDefaultCategory;
            _tmpDefaultCategory = _cursor.getString(_cursorIndexOfDefaultCategory);
            final String _tmpHanafiRuling;
            if (_cursor.isNull(_cursorIndexOfHanafiRuling)) {
              _tmpHanafiRuling = null;
            } else {
              _tmpHanafiRuling = _cursor.getString(_cursorIndexOfHanafiRuling);
            }
            final String _tmpMalikiRuling;
            if (_cursor.isNull(_cursorIndexOfMalikiRuling)) {
              _tmpMalikiRuling = null;
            } else {
              _tmpMalikiRuling = _cursor.getString(_cursorIndexOfMalikiRuling);
            }
            final String _tmpShafiRuling;
            if (_cursor.isNull(_cursorIndexOfShafiRuling)) {
              _tmpShafiRuling = null;
            } else {
              _tmpShafiRuling = _cursor.getString(_cursorIndexOfShafiRuling);
            }
            final String _tmpHanbaliRuling;
            if (_cursor.isNull(_cursorIndexOfHanbaliRuling)) {
              _tmpHanbaliRuling = null;
            } else {
              _tmpHanbaliRuling = _cursor.getString(_cursorIndexOfHanbaliRuling);
            }
            final String _tmpReason;
            if (_cursor.isNull(_cursorIndexOfReason)) {
              _tmpReason = null;
            } else {
              _tmpReason = _cursor.getString(_cursorIndexOfReason);
            }
            final String _tmpReasonDe;
            if (_cursor.isNull(_cursorIndexOfReasonDe)) {
              _tmpReasonDe = null;
            } else {
              _tmpReasonDe = _cursor.getString(_cursorIndexOfReasonDe);
            }
            final String _tmpSource;
            if (_cursor.isNull(_cursorIndexOfSource)) {
              _tmpSource = null;
            } else {
              _tmpSource = _cursor.getString(_cursorIndexOfSource);
            }
            final String _tmpAlternatives;
            if (_cursor.isNull(_cursorIndexOfAlternatives)) {
              _tmpAlternatives = null;
            } else {
              _tmpAlternatives = _cursor.getString(_cursorIndexOfAlternatives);
            }
            _result = new IngredientEntity(_tmpId,_tmpNameDe,_tmpNameEn,_tmpNameTr,_tmpNameAr,_tmpDefaultCategory,_tmpHanafiRuling,_tmpMalikiRuling,_tmpShafiRuling,_tmpHanbaliRuling,_tmpReason,_tmpReasonDe,_tmpSource,_tmpAlternatives);
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
  public Object searchIngredients(final String query,
      final Continuation<? super List<IngredientEntity>> $completion) {
    final String _sql = "\n"
            + "        SELECT * FROM ingredients \n"
            + "        WHERE name_de LIKE '%' || ? || '%' \n"
            + "        OR name_en LIKE '%' || ? || '%' \n"
            + "        OR name_tr LIKE '%' || ? || '%'\n"
            + "        OR name_ar LIKE '%' || ? || '%'\n"
            + "        OR id LIKE '%' || ? || '%'\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 5);
    int _argIndex = 1;
    _statement.bindString(_argIndex, query);
    _argIndex = 2;
    _statement.bindString(_argIndex, query);
    _argIndex = 3;
    _statement.bindString(_argIndex, query);
    _argIndex = 4;
    _statement.bindString(_argIndex, query);
    _argIndex = 5;
    _statement.bindString(_argIndex, query);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<IngredientEntity>>() {
      @Override
      @NonNull
      public List<IngredientEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfNameDe = CursorUtil.getColumnIndexOrThrow(_cursor, "name_de");
          final int _cursorIndexOfNameEn = CursorUtil.getColumnIndexOrThrow(_cursor, "name_en");
          final int _cursorIndexOfNameTr = CursorUtil.getColumnIndexOrThrow(_cursor, "name_tr");
          final int _cursorIndexOfNameAr = CursorUtil.getColumnIndexOrThrow(_cursor, "name_ar");
          final int _cursorIndexOfDefaultCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "default_category");
          final int _cursorIndexOfHanafiRuling = CursorUtil.getColumnIndexOrThrow(_cursor, "hanafi_ruling");
          final int _cursorIndexOfMalikiRuling = CursorUtil.getColumnIndexOrThrow(_cursor, "maliki_ruling");
          final int _cursorIndexOfShafiRuling = CursorUtil.getColumnIndexOrThrow(_cursor, "shafi_ruling");
          final int _cursorIndexOfHanbaliRuling = CursorUtil.getColumnIndexOrThrow(_cursor, "hanbali_ruling");
          final int _cursorIndexOfReason = CursorUtil.getColumnIndexOrThrow(_cursor, "reason");
          final int _cursorIndexOfReasonDe = CursorUtil.getColumnIndexOrThrow(_cursor, "reason_de");
          final int _cursorIndexOfSource = CursorUtil.getColumnIndexOrThrow(_cursor, "source");
          final int _cursorIndexOfAlternatives = CursorUtil.getColumnIndexOrThrow(_cursor, "alternatives");
          final List<IngredientEntity> _result = new ArrayList<IngredientEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final IngredientEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpNameDe;
            _tmpNameDe = _cursor.getString(_cursorIndexOfNameDe);
            final String _tmpNameEn;
            _tmpNameEn = _cursor.getString(_cursorIndexOfNameEn);
            final String _tmpNameTr;
            if (_cursor.isNull(_cursorIndexOfNameTr)) {
              _tmpNameTr = null;
            } else {
              _tmpNameTr = _cursor.getString(_cursorIndexOfNameTr);
            }
            final String _tmpNameAr;
            if (_cursor.isNull(_cursorIndexOfNameAr)) {
              _tmpNameAr = null;
            } else {
              _tmpNameAr = _cursor.getString(_cursorIndexOfNameAr);
            }
            final String _tmpDefaultCategory;
            _tmpDefaultCategory = _cursor.getString(_cursorIndexOfDefaultCategory);
            final String _tmpHanafiRuling;
            if (_cursor.isNull(_cursorIndexOfHanafiRuling)) {
              _tmpHanafiRuling = null;
            } else {
              _tmpHanafiRuling = _cursor.getString(_cursorIndexOfHanafiRuling);
            }
            final String _tmpMalikiRuling;
            if (_cursor.isNull(_cursorIndexOfMalikiRuling)) {
              _tmpMalikiRuling = null;
            } else {
              _tmpMalikiRuling = _cursor.getString(_cursorIndexOfMalikiRuling);
            }
            final String _tmpShafiRuling;
            if (_cursor.isNull(_cursorIndexOfShafiRuling)) {
              _tmpShafiRuling = null;
            } else {
              _tmpShafiRuling = _cursor.getString(_cursorIndexOfShafiRuling);
            }
            final String _tmpHanbaliRuling;
            if (_cursor.isNull(_cursorIndexOfHanbaliRuling)) {
              _tmpHanbaliRuling = null;
            } else {
              _tmpHanbaliRuling = _cursor.getString(_cursorIndexOfHanbaliRuling);
            }
            final String _tmpReason;
            if (_cursor.isNull(_cursorIndexOfReason)) {
              _tmpReason = null;
            } else {
              _tmpReason = _cursor.getString(_cursorIndexOfReason);
            }
            final String _tmpReasonDe;
            if (_cursor.isNull(_cursorIndexOfReasonDe)) {
              _tmpReasonDe = null;
            } else {
              _tmpReasonDe = _cursor.getString(_cursorIndexOfReasonDe);
            }
            final String _tmpSource;
            if (_cursor.isNull(_cursorIndexOfSource)) {
              _tmpSource = null;
            } else {
              _tmpSource = _cursor.getString(_cursorIndexOfSource);
            }
            final String _tmpAlternatives;
            if (_cursor.isNull(_cursorIndexOfAlternatives)) {
              _tmpAlternatives = null;
            } else {
              _tmpAlternatives = _cursor.getString(_cursorIndexOfAlternatives);
            }
            _item = new IngredientEntity(_tmpId,_tmpNameDe,_tmpNameEn,_tmpNameTr,_tmpNameAr,_tmpDefaultCategory,_tmpHanafiRuling,_tmpMalikiRuling,_tmpShafiRuling,_tmpHanbaliRuling,_tmpReason,_tmpReasonDe,_tmpSource,_tmpAlternatives);
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
  public Object getIngredientsByCategory(final String category,
      final Continuation<? super List<IngredientEntity>> $completion) {
    final String _sql = "SELECT * FROM ingredients WHERE default_category = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, category);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<IngredientEntity>>() {
      @Override
      @NonNull
      public List<IngredientEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfNameDe = CursorUtil.getColumnIndexOrThrow(_cursor, "name_de");
          final int _cursorIndexOfNameEn = CursorUtil.getColumnIndexOrThrow(_cursor, "name_en");
          final int _cursorIndexOfNameTr = CursorUtil.getColumnIndexOrThrow(_cursor, "name_tr");
          final int _cursorIndexOfNameAr = CursorUtil.getColumnIndexOrThrow(_cursor, "name_ar");
          final int _cursorIndexOfDefaultCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "default_category");
          final int _cursorIndexOfHanafiRuling = CursorUtil.getColumnIndexOrThrow(_cursor, "hanafi_ruling");
          final int _cursorIndexOfMalikiRuling = CursorUtil.getColumnIndexOrThrow(_cursor, "maliki_ruling");
          final int _cursorIndexOfShafiRuling = CursorUtil.getColumnIndexOrThrow(_cursor, "shafi_ruling");
          final int _cursorIndexOfHanbaliRuling = CursorUtil.getColumnIndexOrThrow(_cursor, "hanbali_ruling");
          final int _cursorIndexOfReason = CursorUtil.getColumnIndexOrThrow(_cursor, "reason");
          final int _cursorIndexOfReasonDe = CursorUtil.getColumnIndexOrThrow(_cursor, "reason_de");
          final int _cursorIndexOfSource = CursorUtil.getColumnIndexOrThrow(_cursor, "source");
          final int _cursorIndexOfAlternatives = CursorUtil.getColumnIndexOrThrow(_cursor, "alternatives");
          final List<IngredientEntity> _result = new ArrayList<IngredientEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final IngredientEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpNameDe;
            _tmpNameDe = _cursor.getString(_cursorIndexOfNameDe);
            final String _tmpNameEn;
            _tmpNameEn = _cursor.getString(_cursorIndexOfNameEn);
            final String _tmpNameTr;
            if (_cursor.isNull(_cursorIndexOfNameTr)) {
              _tmpNameTr = null;
            } else {
              _tmpNameTr = _cursor.getString(_cursorIndexOfNameTr);
            }
            final String _tmpNameAr;
            if (_cursor.isNull(_cursorIndexOfNameAr)) {
              _tmpNameAr = null;
            } else {
              _tmpNameAr = _cursor.getString(_cursorIndexOfNameAr);
            }
            final String _tmpDefaultCategory;
            _tmpDefaultCategory = _cursor.getString(_cursorIndexOfDefaultCategory);
            final String _tmpHanafiRuling;
            if (_cursor.isNull(_cursorIndexOfHanafiRuling)) {
              _tmpHanafiRuling = null;
            } else {
              _tmpHanafiRuling = _cursor.getString(_cursorIndexOfHanafiRuling);
            }
            final String _tmpMalikiRuling;
            if (_cursor.isNull(_cursorIndexOfMalikiRuling)) {
              _tmpMalikiRuling = null;
            } else {
              _tmpMalikiRuling = _cursor.getString(_cursorIndexOfMalikiRuling);
            }
            final String _tmpShafiRuling;
            if (_cursor.isNull(_cursorIndexOfShafiRuling)) {
              _tmpShafiRuling = null;
            } else {
              _tmpShafiRuling = _cursor.getString(_cursorIndexOfShafiRuling);
            }
            final String _tmpHanbaliRuling;
            if (_cursor.isNull(_cursorIndexOfHanbaliRuling)) {
              _tmpHanbaliRuling = null;
            } else {
              _tmpHanbaliRuling = _cursor.getString(_cursorIndexOfHanbaliRuling);
            }
            final String _tmpReason;
            if (_cursor.isNull(_cursorIndexOfReason)) {
              _tmpReason = null;
            } else {
              _tmpReason = _cursor.getString(_cursorIndexOfReason);
            }
            final String _tmpReasonDe;
            if (_cursor.isNull(_cursorIndexOfReasonDe)) {
              _tmpReasonDe = null;
            } else {
              _tmpReasonDe = _cursor.getString(_cursorIndexOfReasonDe);
            }
            final String _tmpSource;
            if (_cursor.isNull(_cursorIndexOfSource)) {
              _tmpSource = null;
            } else {
              _tmpSource = _cursor.getString(_cursorIndexOfSource);
            }
            final String _tmpAlternatives;
            if (_cursor.isNull(_cursorIndexOfAlternatives)) {
              _tmpAlternatives = null;
            } else {
              _tmpAlternatives = _cursor.getString(_cursorIndexOfAlternatives);
            }
            _item = new IngredientEntity(_tmpId,_tmpNameDe,_tmpNameEn,_tmpNameTr,_tmpNameAr,_tmpDefaultCategory,_tmpHanafiRuling,_tmpMalikiRuling,_tmpShafiRuling,_tmpHanbaliRuling,_tmpReason,_tmpReasonDe,_tmpSource,_tmpAlternatives);
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
  public Object getIngredientCount(final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(*) FROM ingredients";
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
