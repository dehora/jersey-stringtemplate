/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.dehora.jst.resource;

import com.sun.jersey.spi.resource.PerRequest;
import com.sun.jersey.api.view.Viewable;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.HashMap;

@Path("/{view}")
@PerRequest
public class Pages
{
    @GET
    @Produces(MediaType.TEXT_HTML)
    public Viewable get(
            @DefaultValue("home")
            @PathParam("view") final String view) throws Exception {
        HashMap<String, Object> templateModel = new HashMap<String, Object>() {{
                put("viewName", view);
            }};
        return new Viewable("/" + view + ".st", templateModel);
    }

}