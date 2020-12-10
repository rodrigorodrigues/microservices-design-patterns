import React from 'react';
import Footer from 'rc-footer';

const FooterContent = () => {
    return (
        <div>
            <Footer
            columns={[
                {
                    items: [{
                        icon: (
                            <i className="fa fa-fw fa-github" style={{ fontSize: '1.5em' }} />
                        ),
                        url: 'https://github.com/rodrigorodrigues/microservices-design-patterns',
                        description: 'GitHub',
                        openExternal: true
                    },
                    {
                        title: 'Environment',
                        description: (<b>{process.env.NODE_ENV}</b>),
                    },
                    {
                        title: 'URL Api',
                        description: (<b>{process.env.REACT_APP_GATEWAY_URL}</b>),
                    },
                    {
                        title: 'App Version',
                        description: (<b>{process.env.REACT_APP_VERSION}</b>),
                    }]
                },
            ]}
                bottom="Microservice Design Patterns"
                theme="light"
            />
        </div>
    )
}

export default FooterContent