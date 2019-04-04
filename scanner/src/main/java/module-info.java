module ca.on.oicr.gsi.runscanner.server {
  exports ca.on.oicr.gsi.runscanner.server;

  requires server.utils;
  requires ca.on.oicr.gsi.runscanner.processorapi;
  requires com.fasterxml.jackson.databind;
  requires com.fasterxml.jackson.datatype.jsr310;
  requires simpleclient;
  requires java.xml;
  requires slf4j.api;
  requires undertow.core;
  requires undertow.servlet;
  requires simpleclient.hotspot;
  requires simpleclient.common;
  requires com.fasterxml.jackson.annotation;
  requires httpclient;
  requires httpcore;
}
