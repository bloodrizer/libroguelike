#version 110

void main(void)
{
   gl_Position = ftransform(); //Transform the vertex position
   gl_MultiTexCoord0;
   
   gl_PointSize = 1400.0 / gl_Position.w;
}
