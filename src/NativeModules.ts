import { NativeModules, requireNativeComponent, ViewProps } from 'react-native';

interface CameraModuleType {
  getBackCameras(): Promise<string[]>;
  startCamera(cameraId: string, textureId: number): Promise<void>;
  takePicturesSimultaneously(): Promise<string[]>;
  stopCamera(cameraId: string): void;
}

interface CameraPreviewProps extends ViewProps {
  cameraId: string;
}

export const CameraModule = NativeModules.CameraModule as CameraModuleType;
export const CameraPreview = requireNativeComponent<CameraPreviewProps>('CameraPreview'); 