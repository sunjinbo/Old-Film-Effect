#extension GL_OES_EGL_image_external : require

precision mediump float;

varying vec2 textureCoordinate;

uniform samplerExternalOES s_texture;

void main() {
    highp vec2 coord = vec2(1.0 - textureCoordinate.y, textureCoordinate.x);
    gl_FragColor=texture2D(s_texture, coord);
}