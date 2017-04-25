<%--
  #%L
  Wildfly Camel :: Example :: Camel Mail
  %%
  Copyright (C) 2013 - 2015 RedHat
  %%
  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
  #L%
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<html>
<head>
    <title>WildFly Camel Subsystem Mail Example</title>
    <link href="bootstrap.min.css" rel="stylesheet"/>
</head>
<body>
<div class="container">
    <div class="page-header">
        <h1>Send an Email</h1>
    </div>
    <form action="/example-camel-mail/send" method="post">
        <div class="form-group">
            <label for="from">From</label>
            <input type="email" class="form-control" id="from" name="from"/>
        </div>
        <div class="form-group">
          <label for="to">To</label>
          <input type="email" class="form-control" id="to" name="to" value="user2@localhost" disabled="disabled"/>
          <input type="hidden" name="to" value="user2@localhost" />
        </div>
        <div class="form-group">
          <label for="subject">Subject</label>
          <input type="text" class="form-control" id="subject" name="subject"/>
        </div>
        <div class="form-group">
          <label for="message">Message</label>
          <textarea class="form-control" id="message" name="message"></textarea>
        </div>
        <div class="form-group">
          <input type="submit" class="btn btn-primary" value="Send"/>
        </div>
    </form>
</div>

<script src="https://ajax.googleapis.com/ajax/libs/jquery/1.11.1/jquery.min.js"></script>
</body>
</html>
