package org.dbtag.data;

import android.graphics.Canvas;
import android.graphics.Color;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.CharacterStyle;

import java.lang.reflect.Array;

/**
 * A simple way of building an object that implements {@link android.text.Spanned}.
 * Our own code because we want the best performance.
 */
public class SpannedBuilder implements Spanned
  {
  private String text_;
  private Object[] spans_;
  private int[] starts_, ends_, flags_;
  private int count_;
  
  public void setText(String text)
    {
    text_ = text;
    }
  
  public void append(int start, int end, int flags, Object what)
    {
    starts_ = append(starts_, count_, start); 
    ends_ = append(ends_, count_, end);
    flags_ = append(flags_, count_, flags);
    spans_ = append(spans_, count_, what);
    ++count_;
    }
  
  private static int[] append(int[] array, int currentSize, int element)
    {
    if (array == null || currentSize + 1 > array.length)
      {
      int[] newArray = new int[currentSize <= 4 ? 8 : currentSize * 2];
      if (array != null)  System.arraycopy(array, 0, newArray, 0, currentSize);
      array = newArray;
      }
    array[currentSize] = element;
    return array;
    }
    
  private static <T> T[] append(T[] array, int currentSize, T element)
    {
    if (array == null || currentSize + 1 > array.length)
      {
      @SuppressWarnings("unchecked")
      T[] newArray = (T[]) new Object[currentSize <= 4 ? 8 : currentSize * 2];
      if (array != null)  System.arraycopy(array, 0, newArray, 0, currentSize);
      array = newArray;
      }
    array[currentSize] = element;
    return array;
    }
    
  @Override public int length() { return text_.length(); }
  @Override public char charAt(int index) { return text_.charAt(index); }
  @Override public CharSequence subSequence(int start, int end) { return text_.subSequence(start, end); }

  @Override public String toString() { return text_; }
  
  @SuppressWarnings({ "unchecked", "null" })
  @Override
  public <T> T[] getSpans(int queryStart, int queryEnd, Class<T> kind)
    {
    int count = 0;
    T[] ret = null;
    T ret1 = null;
    for (int i = 0; i < count_; ++i)
      {
      int spanStart = starts_[i];
      if (spanStart > queryEnd)
          continue;
      int spanEnd = ends_[i];
      if (spanEnd < queryStart)
          continue;
      if (spanStart != spanEnd && queryStart != queryEnd && (spanStart == queryEnd || spanEnd == queryStart))
        continue;
  
      // Expensive test, should be performed after the previous tests
      if (!kind.isInstance(spans_[i])) 
        continue;
  
      if (count == 0)
        {
        ret1 = (T) spans_[i];
        ++count;
        }
      else
        {
        if (count == 1)
          {
          ret = (T[]) Array.newInstance(kind, count_ - i + 1);
          ret[0] = ret1;
          }
        // Safe conversion thanks to the isInstance test above
        ret[count++] = (T) spans_[i];
        }
      }
  
    if (count == 0)
      return (T[]) Array.newInstance(kind, 0);
    if (count == 1)
     {
      // Safe conversion, but requires a suppressWarning
      ret = (T[]) Array.newInstance(kind, 1);
      ret[0] = ret1;
      return ret;
      }
    if (count == ret.length)
      return ret;
  
    T[] nret = (T[]) Array.newInstance(kind, count);
    System.arraycopy(ret, 0, nret, 0, count);
    return nret;
    }
  
  @Override
  public int getSpanStart(Object tag)
    {
    for (int i = 0; i < count_; ++i)
     if (spans_[i] == tag) 
      return starts_[i];
    return -1;
    }
  
  @Override
  public int getSpanEnd(Object tag)
    {
    for (int i = 0; i < count_; ++i)
     if (spans_[i] == tag) 
      return ends_[i];
    return -1;
    }
  
  @Override
  public int getSpanFlags(Object tag)
    {
    for (int i = 0; i < count_; ++i)
     if (spans_[i] == tag) 
      return flags_[i];
    return 0;
    }
  
  @Override
  public int nextSpanTransition(int start, int limit, Class type)
    {
    if (type == null)
      type = Object.class;
    for (int i = 0; i < count_; ++i)
      {
      int st = starts_[i], en = ends_[i];
      if (st > start && st < limit && type.isInstance(spans_[i]))
        limit = st;
      if (en > start && en < limit && type.isInstance(spans_[i]))
        limit = en;
      }
    return limit;
    }

  /**
   * Draw using any CharacterStyle's attached to spans
   */
  public void drawText(Canvas canvas, int start, int end, int l, int t, int b, TextPaint paint, TextPaint tp)
    {
    final int textHeight = (int) paint.getTextSize();
    for (int i = 0; i < count_; ++i)
      {
      int e = ends_[i];
      if (start >= e)
        continue;
      int s = starts_[i];
      if (end <= s)
        break;
      Object span = spans_[i];
      if (!(span instanceof CharacterStyle))
        continue;  // only these affect how we draw text

      if (start < s)
        {
        canvas.drawText(text_, start, s, l, t + textHeight, paint);
        l += (int) paint.measureText(text_, start, s);
        start = s;
        }
      else if (start > s)
        s = start;
      if (e > end)
        e = end;

      // Reset the given temporary TextPaint to the default.  We need a temporary because updateDrawState will change things.
      tp.set(paint);
      ((CharacterStyle) span).updateDrawState(tp);
      int bgColor = tp.bgColor;
      if (bgColor != Color.TRANSPARENT)
        {
        int oldColor = tp.getColor();
        tp.setColor(bgColor);
        int w1 = (int) tp.measureText(text_, s, e);
        canvas.drawRect(l, t, l + w1, b, tp);
        tp.setColor(oldColor);
        }
      canvas.drawText(text_, s, e, l, t + textHeight, tp);
      l += (int) tp.measureText(text_, s, e);
      start = e;
      }
    if (start < end)
      canvas.drawText(text_, start, end, l, t + textHeight, paint);
    }
  }
