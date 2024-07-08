import React from 'react';
import { View, Image, StyleSheet, Alert } from 'react-native';
import {
  startService,
  onNewImage,
} from '@consolecodea/react-native-mtp-camera';

const MyComponent = () => {
  const [image, setImage] = React.useState<string>('');
  React.useEffect(() => {
    loadImagesFromDevice();
    onNewImage((e: any) => setImage(`data:image/jpeg;base64,${e?.imageData}`));
  }, []);

  const loadImagesFromDevice = async () => {
    try {
      let res = await startService();
      console.log('Image loading service started.' + res);
    } catch (error: any) {
      let errorMessage = 'An error occurred while loading images.';
      if (
        error &&
        error instanceof Array &&
        error.length > 0 &&
        error[0].Error
      ) {
        errorMessage = error[0].Error.toString(); // Assuming Error is a string
      } else if (error && error.message) {
        errorMessage = error.message.toString(); // Fallback to error message if available
      }
      Alert.alert('Error', errorMessage);
    }
  };

  return (
    <View style={styles.container}>
      {image && (
        <Image
          source={{ uri: image }}
          style={styles.imageContainer}
          resizeMode="contain"
        />
      )}
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  imageContainer: { height: '100%', width: '100%' },
});

export default MyComponent;
