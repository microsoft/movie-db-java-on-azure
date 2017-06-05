/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

var Jimp = require("jimp");

module.exports = (context) => {

  context.log('Starting...');

  // Read image with Jimp
  Jimp.read(context.bindings.inputBlob).then((image) => {

    context.log('Processing...');

    // Resize image
    image
      .resize(60, Jimp.AUTO)
      .getBuffer(Jimp.MIME_JPEG, (error, stream) => {
        if (error) {
          context.log('There was an error processing the image.');
          context.done(error);
        } else {
          context.log('Node.JS blob trigger function resized ' + context.bindingData.name + ' to ' + image.bitmap.width + 'x' + image.bitmap.height);
          context.bindings.outputBlob = stream;
          context.done();
        }
      });

  }).catch(function (error) {
    context.log(error);
    context.done(error);
  });

};
