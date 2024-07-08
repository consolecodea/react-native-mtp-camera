import React from 'react';
import { View, Image, StyleSheet } from 'react-native';
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
    } catch (error) {
      console.error('Failed to start image loading service:', error);
    }
  };

  return (
    <View style={styles.container}>
      <Image
        source={{ uri: image }}
        style={styles.imageContainer}
        resizeMode="contain"
      />
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
