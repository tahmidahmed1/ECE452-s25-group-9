"""add latitude and longitude to events

Revision ID: 20250701_01
Revises: 
Create Date: 2025-07-01

"""
from alembic import op
import sqlalchemy as sa

# revision identifiers, used by Alembic.
revision = '20250701_01'
down_revision = None
branch_labels = None
depends_on = None

def upgrade():
    op.add_column('events', sa.Column('latitude', sa.Float(), nullable=True))
    op.add_column('events', sa.Column('longitude', sa.Float(), nullable=True))


def downgrade():
    op.drop_column('events', 'longitude')
    op.drop_column('events', 'latitude') 
