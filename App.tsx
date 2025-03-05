/**
 * Sample React Native App
 * https://github.com/facebook/react-native
 *
 * @format
 */

import React, { useEffect, useState } from 'react';
import {
  StyleSheet,
  View,
  TouchableOpacity,
  Text,
  Alert,
  SafeAreaView,
  StatusBar,
  ActivityIndicator,
  PermissionsAndroid,
  Platform,
} from 'react-native';

import { CameraModule, CameraPreview } from './src/NativeModules';

class ErrorBoundary extends React.Component<{ children: React.ReactNode }> {
  state: { hasError: boolean; error: Error | null } = { 
    hasError: false, 
    error: null 
  };

  static getDerivedStateFromError(error: any) {
    return { hasError: true, error };
  }

  render() {
    if (this.state.hasError) {
      return (
        <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center' }}>
          <Text>Something went wrong!</Text>
          <Text>{this.state.error?.toString()}</Text>
        </View>
      );
    }
    return this.props.children;
  }
}

function App(): React.JSX.Element {
  const [cameras, setCameras] = useState<string[]>([]);
  const [isCapturing, setIsCapturing] = useState(false);
  const [lastCapturePaths, setLastCapturePaths] = useState<string[]>([]);

  useEffect(() => {
    const initializeCameras = async () => {
      try {
        const granted = await PermissionsAndroid.request(
          PermissionsAndroid.PERMISSIONS.CAMERA,
          {
            title: "Camera Permission",
            message: "App needs camera access to take pictures",
            buttonNeutral: "Ask Me Later",
            buttonNegative: "Cancel",
            buttonPositive: "OK"
          }
        );

        if (granted === PermissionsAndroid.RESULTS.GRANTED) {
          // Get only back cameras
          const backCameraIds = await CameraModule.getBackCameras();
          console.log('Available back cameras:', backCameraIds);
          setCameras(backCameraIds);
        }
      } catch (err) {
        console.error('Error initializing cameras:', err);
        Alert.alert('Error', err instanceof Error ? err.message : String(err));
      }
    };

    initializeCameras();
  }, []);

  const takePictures = async () => {
    try {
      setIsCapturing(true);
      const paths = await CameraModule.takePicturesSimultaneously();
      setLastCapturePaths(paths);
    } catch (err: unknown) {
      console.error('Failed to capture images:', err);
      Alert.alert('Error', err instanceof Error ? err.message : String(err));
    } finally {
      setIsCapturing(false);
    }
  };

  return (
    <SafeAreaView style={styles.container}>
      <StatusBar barStyle="dark-content" />
      <View style={styles.previewContainer}>
        {cameras.map((cameraId) => (
          <View key={cameraId} style={styles.preview}>
            <CameraPreview 
              style={styles.previewCamera} 
              cameraId={cameraId}
            />
          </View>
        ))}
      </View>
      
      <View style={styles.controls}>
        <TouchableOpacity
          style={[styles.captureButton, isCapturing && styles.captureButtonDisabled]}
          onPress={takePictures}
          disabled={isCapturing}
        >
          <View style={styles.captureButtonInner} />
        </TouchableOpacity>
        {isCapturing && <ActivityIndicator size="large" color="#fff" />}
      </View>

      {lastCapturePaths.length > 0 && (
        <Text style={styles.captureInfo}>
          Last capture saved to: {lastCapturePaths.join(', ')}
        </Text>
      )}
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: 'black',
  },
  previewContainer: {
    flex: 1,
    flexDirection: 'row',
    flexWrap: 'wrap',
  },
  preview: {
    width: '50%',
    aspectRatio: 3/4,
    padding: 2,
  },
  previewCamera: {
    width: '100%',
    height: '100%',
    backgroundColor: 'transparent',
  },
  controls: {
    position: 'absolute',
    bottom: 0,
    left: 0,
    right: 0,
    padding: 20,
    alignItems: 'center',
  },
  captureButton: {
    width: 80,
    height: 80,
    borderRadius: 40,
    backgroundColor: '#fff',
    justifyContent: 'center',
    alignItems: 'center',
  },
  captureButtonDisabled: {
    opacity: 0.7,
  },
  captureButtonInner: {
    width: 64,
    height: 64,
    borderRadius: 32,
    borderWidth: 2,
    borderColor: '#000',
  },
  captureInfo: {
    color: '#fff',
    marginBottom: 10,
    backgroundColor: 'rgba(0,0,0,0.6)',
    padding: 8,
    borderRadius: 8,
  },
});

export default App;
