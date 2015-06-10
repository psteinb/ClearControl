package rtlib.cameras;

import net.imglib2.img.basictypeaccess.array.ArrayDataAccess;
import net.imglib2.type.NativeType;
import rtlib.core.device.NameableInterface;
import rtlib.core.variable.booleanv.BooleanVariable;
import rtlib.core.variable.doublev.DoubleVariable;
import rtlib.core.variable.objectv.ObjectVariable;
import rtlib.stack.StackInterface;

public interface StackCameraDeviceInterface<T extends NativeType<T>, A extends ArrayDataAccess<A>>	extends
																																																		CameraDeviceInterface,
																																																		NameableInterface
{
	DoubleVariable getNumberOfImagesPerPlaneVariable();

	BooleanVariable getStackModeVariable();

	ObjectVariable<StackInterface<T, A>> getStackVariable();

	@Override
	void trigger();

}
