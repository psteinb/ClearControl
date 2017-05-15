package clearcontrol.gui.video.video3d;

import java.util.concurrent.TimeUnit;

import clearcontrol.core.concurrent.asyncprocs.AsynchronousProcessorBase;
import clearcontrol.core.device.VirtualDevice;
import clearcontrol.core.variable.Variable;
import clearcontrol.gui.video.StackDisplayInterface;
import clearcontrol.gui.video.util.WindowControl;
import clearcontrol.stack.EmptyStack;
import clearcontrol.stack.StackInterface;
import clearcontrol.stack.metadata.MetaDataOrdinals;
import cleargl.ClearGLWindow;
import clearvolume.renderer.ClearVolumeRendererInterface;
import clearvolume.renderer.cleargl.ClearGLVolumeRenderer;
import clearvolume.renderer.factory.ClearVolumeRendererFactory;
import coremem.ContiguousMemoryInterface;
import coremem.enums.NativeTypeEnum;
import coremem.util.Size;

public class Stack3DDisplay extends VirtualDevice
                            implements StackDisplayInterface
{
  private static final int cDefaultDisplayQueueLength = 2;
  protected static final long cTimeOutForBufferCopy = 5;

  private ClearVolumeRendererInterface mClearVolumeRenderer;

  private final Variable<StackInterface> mInputStackVariable;
  private Variable<StackInterface> mOutputStackVariable;

  private AsynchronousProcessorBase<StackInterface, Object> mAsynchronousDisplayUpdater;

  private volatile Variable<Boolean> mVisibleVariable;
  private volatile Variable<Boolean> mWaitForLastChannel;

  public Stack3DDisplay(final String pWindowName)
  {
    this(pWindowName, 512, 512, 1, cDefaultDisplayQueueLength);
  }

  public Stack3DDisplay(final String pWindowName,
                        final int pWindowWidth,
                        final int pWindowHeight,
                        final int pNumberOfLayers,
                        final int pUpdaterQueueLength)
  {
    super(pWindowName);

    NativeTypeEnum lNativeTypeEnum = NativeTypeEnum.UnsignedShort;

    mClearVolumeRenderer =
                         ClearVolumeRendererFactory.newBestRenderer(pWindowName,
                                                                    pWindowWidth,
                                                                    pWindowHeight,
                                                                    lNativeTypeEnum,
                                                                    2048,
                                                                    2048,
                                                                    pNumberOfLayers,
                                                                    false);

    mVisibleVariable = new Variable<Boolean>("Visible", false);

    mVisibleVariable.addSetListener((o, n) -> {
      mClearVolumeRenderer.setVisible(n);
    });

    setVisible(false);

    mClearVolumeRenderer.setAdaptiveLODActive(true);
    mClearVolumeRenderer.disableClose();

    if (mClearVolumeRenderer instanceof ClearGLVolumeRenderer)
    {
      ClearGLVolumeRenderer lClearGLVolumeRenderer =
                                                   (ClearGLVolumeRenderer) mClearVolumeRenderer;
      ClearGLWindow lClearGLWindow =
                                   lClearGLVolumeRenderer.getClearGLWindow();
      lClearGLWindow.addWindowListener(new WindowControl(lClearGLWindow));
    }

    class Aynchronous3DStackDisplayUpdater extends
                                           AsynchronousProcessorBase<StackInterface, Object>
    {
      public Aynchronous3DStackDisplayUpdater(String pName,
                                              int pMaxQueueSize)
      {
        super(pName, pMaxQueueSize);
      }

      @Override
      public Object process(final StackInterface pStack)
      {
        if (pStack instanceof EmptyStack)
          return null;

        if (mClearVolumeRenderer.isShowing() && pStack.getDepth() > 1)
        {
          // info("received stack: " + pStack);

          if (pStack.getMetaData().hasValue("NoDisplay"))
          {
            info("Received stack with NoDisplay metadata value");
            return null;
          }

          final long lSizeInBytes = pStack.getSizeInBytes();
          final long lWidth = pStack.getWidth();
          final long lHeight = pStack.getHeight();
          final long lDepth = pStack.getDepth();

          final NativeTypeEnum lNativeTypeEnum =
                                               mClearVolumeRenderer.getNativeType();
          final long lBytesPerVoxel = Size.of(lNativeTypeEnum);

          int lChannel = 0;
          Long lChannelObj =
                           pStack.getMetaData()
                                 .getValue(MetaDataOrdinals.DisplayChannel);
          if (lChannelObj != null)
            lChannel = lChannelObj.intValue() % pNumberOfLayers;

          if (lWidth * lHeight
              * lDepth
              * lBytesPerVoxel != lSizeInBytes)
          {
            System.err.println(Stack3DDisplay.class.getSimpleName()
                               + ": receiving wrong pointer size!");
            return null;
          }

          final ContiguousMemoryInterface lContiguousMemory =
                                                            pStack.getContiguousMemory();

          if (lContiguousMemory.isFree())
          {
            System.err.println(Stack3DDisplay.class.getSimpleName()
                               + ": buffer released!");
            return null;
          }

          Double lVoxelWidth = pStack.getMetaData().getVoxelDimX();
          Double lVoxelHeight = pStack.getMetaData().getVoxelDimY();
          Double lVoxelDepth = pStack.getMetaData().getVoxelDimZ();

          if (lVoxelWidth == null)
          {
            lVoxelWidth = 1.0 / lWidth;
            warning("No voxel width provided, using 1.0 instead.");

          }

          if (lVoxelHeight == null)
          {
            lVoxelHeight = 1.0 / lHeight;
            warning("No voxel height provided, using 1.0 instead.");

          }

          if (lVoxelDepth == null)
          {
            lVoxelDepth = 1.0 / lDepth;
            warning("No voxel depth provided, using 1.0 instead.");

          }

          mClearVolumeRenderer.setVolumeDataBuffer(lChannel,
                                                   lContiguousMemory,
                                                   lWidth,
                                                   lHeight,
                                                   lDepth,
                                                   lVoxelWidth,
                                                   lVoxelHeight,
                                                   lVoxelDepth);

          // FIXME
          /*
          pStack.getVoxelSizeInRealUnits(0),
          pStack.getVoxelSizeInRealUnits(1),
          pStack.getVoxelSizeInRealUnits(2)); /**/

          if (mWaitForLastChannel.get()
              && ((lChannel + 1)
                  % mClearVolumeRenderer.getNumberOfRenderLayers()) == 0)
          {
            mClearVolumeRenderer.waitToFinishAllDataBufferCopy(cTimeOutForBufferCopy,
                                                               TimeUnit.SECONDS);/**/
          }
          else
            mClearVolumeRenderer.waitToFinishDataBufferCopy(lChannel,
                                                            cTimeOutForBufferCopy,
                                                            TimeUnit.SECONDS);/**/
        }

        forwardStack(pStack);

        return null;
      }
    }

    mAsynchronousDisplayUpdater =
                                new Aynchronous3DStackDisplayUpdater("AsynchronousDisplayUpdater-"
                                                                     + pWindowName,
                                                                     pUpdaterQueueLength);

    mInputStackVariable = new Variable<StackInterface>("3DStackInput")
    {

      @Override
      public StackInterface setEventHook(final StackInterface pOldStack,
                                         final StackInterface pNewStack)
      {
        if (!mAsynchronousDisplayUpdater.passOrFail(pNewStack))
        {
          forwardStack(pNewStack);
        }

        return super.setEventHook(pOldStack, pNewStack);
      }

    };

    mWaitForLastChannel = new Variable<Boolean>("WaitForLastChannel",
                                                false);

  }

  private void forwardStack(final StackInterface pNewStack)
  {
    if (mOutputStackVariable != null)
      mOutputStackVariable.set(pNewStack);
    else if (!pNewStack.isReleased())
      pNewStack.release();
  }

  @Override
  public Variable<StackInterface> getOutputStackVariable()
  {
    return mOutputStackVariable;
  }

  @Override
  public void setOutputStackVariable(Variable<StackInterface> pOutputStackVariable)
  {
    mOutputStackVariable = pOutputStackVariable;
  }

  public Variable<Boolean> getVisibleVariable()
  {
    return mVisibleVariable;
  }

  @Override
  public Variable<StackInterface> getInputStackVariable()
  {
    return mInputStackVariable;
  }

  public void setVisible(final boolean pIsVisible)
  {
    mVisibleVariable.set(pIsVisible);
  }

  @Override
  public boolean open()
  {
    mAsynchronousDisplayUpdater.start();
    return false;
  }

  @Override
  public boolean close()
  {
    try
    {
      mAsynchronousDisplayUpdater.stop();
      mAsynchronousDisplayUpdater.waitToFinish(1, TimeUnit.SECONDS);
      mAsynchronousDisplayUpdater.close();
      mClearVolumeRenderer.waitToFinishAllDataBufferCopy(1,
                                                         TimeUnit.SECONDS);
      if (mClearVolumeRenderer != null)
        mClearVolumeRenderer.close();
      return true;
    }
    catch (final Throwable e)
    {
      e.printStackTrace();
      return false;
    }
  }

  public boolean isVisible()
  {
    return mClearVolumeRenderer.isShowing();
  }

  public void requestFocus()
  {
    if (mClearVolumeRenderer instanceof ClearGLVolumeRenderer)
    {
      ClearGLVolumeRenderer lClearGLVolumeRenderer =
                                                   (ClearGLVolumeRenderer) mClearVolumeRenderer;
      ClearGLWindow lClearGLWindow =
                                   lClearGLVolumeRenderer.getClearGLWindow();
      lClearGLWindow.requestFocus();
    }
  }

  public void disableClose()
  {
    mClearVolumeRenderer.disableClose();
  }

  public Variable<Boolean> getWaitForLastChannel()
  {
    return mWaitForLastChannel;
  }

  public void setWaitForLastChannel(Variable<Boolean> pWaitForLastChannel)
  {
    mWaitForLastChannel = pWaitForLastChannel;
  }

  /**
   * Gets GL window for handling size and position.
   * 
   * @return the GL window
   */
  public ClearGLWindow getGLWindow()
  {
    if (mClearVolumeRenderer instanceof ClearGLVolumeRenderer)
    {
      ClearGLVolumeRenderer lClearGLVolumeRenderer =
                                                   (ClearGLVolumeRenderer) mClearVolumeRenderer;
      return lClearGLVolumeRenderer.getClearGLWindow();
    }
    else
      return null;
  }
}
